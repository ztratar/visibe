(ns ^{:doc "For collection of twitter data."}
  visibe.feeds.twitter.dev
  (:require [clojure.data.json :as json]
            [clj-http.lite.client :as client]
            [clojure.string :as s]
            [visibe.homeless :refer [sort-datums-by-timestamp]]
            [visibe.feeds.storage :refer [append-datums]]
            [clojure.data.codec.base64 :as b64]            
            [visibe.state :refer [state update-state!]]))

;;; NOTE, Thu Oct 03 2013, Francis Wolke
;;; If you want to understand what this code is doing, read these:

;;; https://dev.twitter.com/docs/using-search
;;; https://dev.twitter.com/docs/working-with-timelines
;;; https://dev.twitter.com/docs/api/1.1/get/search/tweets

;;; XXX, Tue Oct 08 2013, Francis Wolke
;;; As the twitter API makes no guarantees about what will be returned from the
;;; time perspective. This code needs to be well tested.

(defn bearer-token []
  (get-in @state [:twitter :bearer-token]))

;;; TODO, Tue Oct 08 2013, Francis Wolke
;;; Move the bearer token into visibe.state

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
  ([query-string] (client/get (str "https://api.twitter.com/1.1/search/tweets.json?q="
                                   ;; http://en.wikipedia.org/wiki/URL_encoding
                                   (s/replace query-string " " "%23")
                                   "&count=100") ; current max is 100 tweets.
                              {:headers {"Authorization" (str "Bearer "
                                                              (bearer-token))}}))

  ([_ & {:keys [query]}] (client/get (str "https://api.twitter.com/1.1/search/tweets.json"
                                          query)
                                     {:headers {"Authorization" (str "Bearer " (bearer-token))}})))

(defn current-trends
  "Convenience function"
  []
  (:trends (:app @state)))

(defn underscore->hyphen [m]
  (zipmap (map #((keyword (clojure.string/replace (str (name %)) "_" "-"))) (keys m)) (vals m)))

(defn tweet->essentials
  ;; FIXME, NOTE Fri Oct 04 2013, Francis Wolke
  ;; `:text` path may not always have full urls.

  ;; Also, should we even be throwing away any data?

  ;; Should we fetch profile pictures on the server side?
  [tweet]
  (underscore->hyphen (merge (select-keys tweet [:text :profile_image_url_https :created_at])
                             (select-keys (:user tweet) [:name :screen_name]))))

(defn track-trend
  "Tracks a trend while it's still an active trend. Runs in future, which 
returns `nil` when trend is no longer in `(current-trends)'"
  [trend]
  ;; NOTE, Fri Oct 04 2013, Francis Wolke
  ;; For the time being, I don't want to deal with the full stream.
  
  ;; Twitter's rate limit window is 15 minutes. We are allowed 450 requests over
  ;; this peiod of time. (/ (* 15 60) 450) => 2 sec
  (future
    (loop [tweet-data (search-tweets trend)]
      ;; 3 min
      (Thread/sleep 180000)
      (let [new-query (:refresh_url (:search_metadata tweet-data))]
        (if-not ((current-trends) trend) nil
                (do (append-datums trend
                                   (sort-datums-by-timestamp (map tweet->essentials (:statuses tweet-data))))
                    (recur (search-tweets :query new-query))))))))
