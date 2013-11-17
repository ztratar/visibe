(ns ^{:doc "For collection of twitter data."}
  visibe.feeds.twitter
  (:use user)
  (:require [clojure.data.json :as json]
            [clojure.set :refer [rename-keys]]
            [org.httpkit.server :as hk]
            [clj-http.lite.client :as client]
            [clojure.string :as s]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-time.core :refer [date-time]]
            [visibe.homeless :refer [rfc822-str->long date-time-str->long]]
            [visibe.feeds.storage :refer [append-datums]]
            [clojure.data.codec.base64 :as b64]            
            [visibe.state :refer [state gis assoc-in-state!]]))

;;; NOTE, Thu Oct 03 2013, Francis Wolke
;;; If you want to understand what this code is doing, read these:

;;; https://dev.twitter.com/docs/using-search
;;; https://dev.twitter.com/docs/working-with-timelines
;;; https://dev.twitter.com/docs/api/1.1/get/search/tweets

;;; NOTE, Sun Oct 13 2013, Francis Wolke

;;; For the time being we are only tracking trends that are in the current
;;; google trends. An implication of this is that if a bunch of users click
;;; through to a trend and are watching it progress and then we stop tracking
;;; it, their data feed will die. This dosn't matter for the time being, but if
;;; we want users to have a good experience, this needs to change in the
;;; future.

(defn twitter-time->long
  "Accepts a twitter time string and returns a string in rfc822 format"
  [s]
  (let [[weekday month day time ? year] (clojure.string/split s #" ")]
    (rfc822-str->long (clojure.string/join " " [(str weekday ",") day month year time ?]))))

(defn bearer-token []
  (get-in @state [:twitter :bearer-token]))

(defn string-to-base64-string [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(defn new-bearer-token!
  "Calls the twitter api and updates state with a new bearer token."
  []
  (let [auth-str (string-to-base64-string
                  (str (:consumer-key (:twitter @state)) ":"
                       (:consumer-secret (:twitter @state))))

        token (-> (client/post "https://api.twitter.com/oauth2/token" 
                               {:headers {"Authorization" (str "Basic " auth-str)
                                          "Content-Type" "application/x-www-form-urlencoded;charset=UTF-8"}
                                :body "grant_type=client_credentials"})
                  (:body)
                  (json/read-json)
                  (:access_token))]
    
    (assoc-in-state! [:twitter :bearer-token] token)))

(defn rate-limits
  "Convenience function. Returns rate limit data"
  ;; NOTE, Fri Oct 04 2013, Francis Wolke
  ;; This actually returns all HTTP routes that you can hit. A comprehensive
  ;; twitter library would use this data to generate functions / options to
  ;; them. 
  []
  (-> (client/get "https://api.twitter.com/1.1/application/rate_limit_status.json"
                  {:headers {"Authorization" (str "Bearer" " " (bearer-token))}})
      (:body)
      (json/read-json)))

(defn search-tweets
  "Searches the twitter api for tweets matching the specified query"
  [query-string]
  (-> (client/get (str "https://api.twitter.com/1.1/search/tweets.json?q="
                       ;; http://en.wikipedia.org/wiki/URL_encoding
                       (s/replace query-string " " "%23")
                       "&count=100")    ; current max is 100 tweets.
                  {:headers {"Authorization" (str "Bearer " (bearer-token))}})
      (:body)
      (json/read-json)))

(defn twitter-q [query]
  (-> (client/get (str "https://api.twitter.com/1.1/search/tweets.json" query)
                  {:headers {"Authorization" (str "Bearer " (bearer-token))}})
      (:body)
      (json/read-json)))

(defn ds->ws-message
  "[d]ata [s]tructure -> websocket message"
  ([ds] (ds->ws-message :print ds)) 
  ([type ds] (str {:type type :data ds})))

(defn prep-tweets-for-storage
  [trend tweets]
  (let [tweets (:statuses tweets)
        tweets (map #(assoc % :trend trend :datum-type :tweet) tweets)
        tweets (map #(rename-keys % {:created_at :created-at}) tweets)
        tweets (map #(update-in % [:created-at] twitter-time->long) tweets)]
    tweets))

(defn store-tweets [trend tweets]
  (append-datums trend (prep-tweets-for-storage trend tweets)))

(defn tweet->essentials
  ;; FIXME, NOTE Fri Oct 04 2013, Francis Wolke
  ;; `:text` path may not always have full urls.
  [tweet]
  (-> (merge (select-keys tweet [:text :created-at :trend :datum-type])
             (select-keys (:user tweet) [:name :screen_name :profile_image_url_https]))))

(defn subscribed-clients [trend]
  (map first (filter (fn [[channel context]] (and (:on context) (some #{trend} (:subscriptions context)))) (seq (gis [:app :channels])))))

(defn push-tweets-to-subscribed-clients! [trend tweets]
  (let [clients (seq (gis [:app :channels]))
        subscribed-clients (when-not (empty? clients) (subscribed-clients trend))]

    (when-not (empty? subscribed-clients) 
      (let [clean-tweets (map #(assoc (tweet->essentials %) :trend trend :datum-type :tweet) tweets)]
        (doseq [client subscribed-clients]
          (hk/send! client (ds->ws-message :datums clean-tweets)))))))

;;; TODO, Thu Nov 14 2013, Francis Wolke
;;; only stop tracking a trend iff it's not current, or subscribed to by any clients.

(defn track-trend
  "Tracks a trend while it's still an 'active' trend. Runs in future, which 
returns `nil` when trend is no longer 'active'. 'Active' is defined as being in
`current-trends'"
  [trend]
  ;; NOTE, Fri Oct 04 2013, Francis Wolke
  ;; For the time being, I don't want to deal with the full stream.
  
  ;; Twitter's rate limit window is 15 minutes. We are allowed 450 requests over
  ;; this peiod of time. (/ (* 15 60) 450) => 2 sec
  (future
    (let [tweet-data (search-tweets trend)]
      (store-tweets trend tweet-data)
      ;; TODO, Thu Nov 14 2013, Francis Wolke
      ;; This initialization can be moved into the loop
      (loop [tweet-data tweet-data]
        (let [new-query (:refresh_url (:search_metadata tweet-data))]
          (when (or (some #{trend} (keys (gis [:google :trends])))
                    (subscribed-clients trend))
            (do (future (push-tweets-to-subscribed-clients! trend (:statuses tweet-data)))
                (store-tweets trend tweet-data)
                (Thread/sleep (/ 180000 3)) ; 1 min
                (recur (twitter-q new-query)))))))))
