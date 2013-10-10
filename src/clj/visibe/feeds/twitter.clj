(ns ^{:doc "For collection of twitter data."}
  visibe.feeds.twitter
  (:require [clojure.data.json :as json]
            [clj-http.lite.client :as client]
            [clojure.string :as s]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-time.core :refer [date-time]]
            [visibe.homeless :refer [sort-datums-by-timestamp]]
            [visibe.feeds.storage :refer [append-datums]]
            [clojure.data.codec.base64 :as b64]            
            [visibe.state :refer [state update-state!]]))

;;; NOTE, Thu Oct 03 2013, Francis Wolke
;;; If you want to understand what this code is doing, read these:

;;; https://dev.twitter.com/docs/using-search
;;; https://dev.twitter.com/docs/working-with-timelines
;;; https://dev.twitter.com/docs/api/1.1/get/search/tweets

(defn twitter->rfc822
  "Accepts a twitter time string and returns a string in rfc822 format"
  [s]
  (let [[weekday month day time ? year] (clojure.string/split s #" ")]
    (clojure.string/join " " [(str weekday ",") day month year time ?])))

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
    
    (update-state! [:twitter :bearer-token] token)))

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

(defn current-trends
  "Convenience function"
  []
  (:trends (:google @state)))

(defn underscore->hyphen [m]
  (letfn [(individual-kwd [kwd] (keyword (clojure.string/replace  (str (name kwd)) "_" "-")))]
    (zipmap (map individual-kwd (keys m)) (vals m))))

(defn tweet->essentials
  ;; FIXME, NOTE Fri Oct 04 2013, Francis Wolke
  ;; `:text` path may not always have full urls.

  ;; Should we even be throwing away any data?

  ;; Should we fetch profile pictures on the server side?
  [tweet]
  (-> (merge (select-keys tweet [:text :profile_image_url_https :created_at])
             (select-keys (:user tweet) [:name :screen_name]))
      (underscore->hyphen)
      (update-in [:created-at] twitter->rfc822)))

(defn- store-tweets
  [trend tweets]
  (append-datums trend (sort-datums-by-timestamp (map tweet->essentials tweets))))

(defn track-trend
  "Tracks a trend while it's still an active trend. Runs in future, which 
returns `nil` when trend is no longer in `(current-trends)'"
  [trend]
  ;; NOTE, Fri Oct 04 2013, Francis Wolke
  ;; For the time being, I don't want to deal with the full stream.
  
  ;; Twitter's rate limit window is 15 minutes. We are allowed 450 requests over
  ;; this peiod of time. (/ (* 15 60) 450) => 2 sec
  (future
    (let [tweet-data (search-tweets trend)
          _ (store-tweets trend (:statuses tweet-data))]
      (loop [tweet-data tweet-data]
        ;; 3 min
        (Thread/sleep 180000)
        (let [new-query (:refresh_url (:search_metadata tweet-data))]
          (when ((set (current-trends)) trend)
            (do (store-tweets trend tweet-data)
                (recur (twitter-q :query new-query)))))))))
