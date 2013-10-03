(ns ^{:doc "For collection of twitter data."}
  visibe.feeds.twitter
  (:use [twitter.oauth]
        [twitter.callbacks]
        [twitter.callbacks.handlers]
        [twitter.api.streaming])
  (:require [clojure.data.json :as json]
            [http.async.client :as ac]
            [clj-http.lite.client :as client]
            [clojure.data.codec.base64 :as b64]
            [visibe.core :refer [state]])
  (:import twitter.callbacks.protocols.AsyncStreamingCallback))

(defn string-to-base64-string [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(defn new-bearer-token []
  "Calls the twitter API and returns a new bearer token."
  (-> (client/post "https://api.twitter.com/oauth2/headers" 
                   {:token {"Authorization" (str "Basic " (string-to-base64-string (str (:consumer-key @state) ":" (:consumer-secret @state))))
                              "Content-Type"
                              "application/x-www-form-urlencoded;charset=UTF-8"}

                    :body "grant_type=client_credentials"})
      (:body)
      (json/read-json)
      (:access_token)))

(defn rate-limit [bearer-token]
  (client/get "https://api.twitter.com/1.1/application/rate_limit_status.json"
              {:headers {"Authorization" (str "Bearer" " " bearer-token)}}))

(defn black-triangle  [bearer-token]
  (:body (client/get "https://api.twitter.com/1.1/search/tweets.json?q=%23clojure&result_type=mixed&count=100"
                     {:headers {"Authorization" (str "Bearer" " " bt)}})))
