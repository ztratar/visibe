(ns visibe.feeds.twitter
  "For collection of twitter data. Relevent API docs:

   https://dev.twitter.com/docs/using-search
   https://dev.twitter.com/docs/working-with-timelines
   https://dev.twitter.com/docs/api/1.1/get/search/tweets"
  (:use user)
  (:require [clojure.data.json :as json]
            [org.httpkit.server :as hk]
            [visibe.api :refer [ds->ws-message]]
            [cemerick.url :refer [url-encode]]
            [visibe.homeless :refer [sleep]]
            [clj-http.lite.client :as client]
            [clojure.string :as s]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-time.core :refer [date-time]]
            [visibe.feeds.storage :refer [append-datums]]
            [clojure.data.codec.base64 :as b64]            
            [visibe.state :refer [state gis assoc-in-state!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Boilerplate

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
      :body
      json/read-json))

(defn search-tweets
  "Searches the twitter api for tweets matching the specified trend"
  [trend]
  (-> (client/get (str "https://api.twitter.com/1.1/search/tweets.json?lang=en&q="
                       (url-encode trend) "&count=100") ; current max is 100 tweets.
                  {:headers {"Authorization" (str "Bearer " (bearer-token))}})
      :body
      json/read-json))

(defn next-page
  "Call with the query string handed back in the metadata for the next page of
   tweet data"
  [query-string]
  (-> (client/get (str "https://api.twitter.com/1.1/search/tweets.json" query-string)
                  {:headers {"Authorization" (str "Bearer " (bearer-token))}})
      :body
      json/read-json))

