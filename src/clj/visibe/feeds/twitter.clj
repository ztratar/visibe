(ns ^{:doc "For collection of twitter data."}
  visibe.feeds.twitter
  (:use [twitter.oauth]
        [twitter.callbacks]
        [twitter.callbacks.handlers]
        [twitter.api.streaming])
  (:require [clojure.data.json :as json]
            [http.async.client :as ac]
            [clj-http.lite.client :as client]
            [clojure.string :as s]
            [clojure.data.codec.base64 :as b64]
            [visibe.core :refer [state]])
  (:import twitter.callbacks.protocols.AsyncStreamingCallback
           java.net.URLEncoder))

;;; TODO, Thu Oct 03 2013, Francis Wolke
;;; https://dev.twitter.com/docs/using-search
;;; https://dev.twitter.com/docs/working-with-timelines
;;; https://dev.twitter.com/docs/api/1.1/get/search/tweets

;;; Perhaps we can use streaming with application only authentication. https://dev.twitter.com/discussions/18517

;;; Use macro magic to remove the bearer-token

;;; Search metadata is interesting, as they pass back the next query that you
;;; can call. 

(defn string-to-base64-string [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(defn new-bearer-token []
  "calls the twitter api and returns a new bearer token."
  ;; fixme, fri oct 04 2013, francis wolke
  ;; relies on implementation details of `state'. make functional.
  (let [auth-str (string-to-base64-string (str (:consumer-key (:twitter @state)) ":"
                                               (:consumer-secret (:twitter @state))))]
    (-> (client/post "https://api.twitter.com/oauth2/token" 
                     {:headers {"Authorization" (str "Basic " auth-str)
                              "Content-Type" "application/x-www-form-urlencoded;charset=UTF-8"}
                      :body "grant_type=client_credentials"})
        (:body)
        (json/read-json)
        (:access_token))))

(defn rate-limit [bearer-token]
  (client/get "https://api.twitter.com/1.1/application/rate_limit_status.json"
              {:headers {"Authorization" (str "Bearer" " " bearer-token)}}))

(defn black-triangle [bearer-token]
  (:body (client/get "https://api.twitter.com/1.1/search/tweets.json?q=%23Miriam%20Carey&result_type=mixed&count=100"
                     {:headers {"Authorization" (str "Bearer" " " bearer-token)}})))

(defn t-q [bearer-token q]
  ;; q=%23clojure&result_type=mixed&count=100
  (:body (client/get (str "https://api.twitter.com/1.1/search/tweets.json")
                     {:headers {"Authorization" (str "Bearer" " "
                                                     bearer-token)}})))



(defn search-tweets
  "Searches the twitter api for tweets matching the specified query"
  [bearer-token queryn]
  (client/get (str "https://api.twitter.com/1.1/search/tweets.json?q="
                   ;; http://en.wikipedia.org/wiki/URL_encoding
                   (s/replace query " " "%23")
                   "&count=100")        ; current max is 1000 tweets.
              {:headers {"Authorization" (str "Bearer " bearer-token)}}))

(defn current-trends
  "Convenience function"
  []
  (:current-trends (:app @state)))

(defn essential-data
  ;; FIXME, Fri Oct 04 2013, Francis Wolke
  ;; Tweet may not always recover full urls.
  [tweet]
  (merge (select-keys tweet :text :profile_image_url_https)
         (select-keys (:user tweet) :name :screen_name)))

(defn track-trend
  "Tracks a trend while it's still an active trend. Runs in future, returns
`nil` when trend is no longer in `(current-trends)'"
  [trend]
  ;; NOTE, Fri Oct 04 2013, Francis Wolke
  ;; For the time being, I don't want to deal with the full stream.
  
  ;; Twitter's rate limit window is 15 minutes. We are allowed 450 requests over
  ;; this peiod of time. (/ (* 15 60) 450) => 2 sec
  (future (fn
            ([] (let [data (search-tweets trend)
                      new-query (:refresh_url (:search_metadata data))
                      tweets (:statuses data)]
                  (Thread/sleep 180000) ; 3 min
                  (when ((current-trends) trend)
                    (do (persist-tweets (map essential-data tweets))
                        (recur new-query)))))
            
            ([query] (let [data (search-tweets :query query)
                           new-query (:refresh_url (:search_metadata data))
                           tweets (:statuses data)]
                       (Thread/sleep 180000) ; 3 min
                       (when ((current-trends) trend)
                         (do (persist-tweets (map essential-data tweets))
                             (recur new-query))))))))
