(ns ^{:doc "For collection of google trends data."}
  visibe.feeds.google-trends  
  (:require [clj-http.lite.client :as client]
            [clojure.data.json :refer [read-json]]
            [cheshire.core :refer [decode]]))

(defn raw-google-trends []
  ;; NOTE, Mon Sep 30 2013, Francis Wolke
  ;; For the time being we don't need phantom.js as they update this when
  ;; google does, and don't appear to care that we're hitting their API.
  (read-json (:body (client/get "http://hawttrends.appspot.com/api/terms/"))))

(def google-mapping
  ;; NOTE, Mon Sep 30 2013, Francis Wolke
  ;; I have no idea why they're ranked like this (and have missing keys). The
  ;; second version of the should be able to figure out countries itself using
  ;; google's translation tools.
  {:1 :united-states
   :3 :india
   :4 :japan
   :5 :singapore
   :6 :israel
   :8 :australia
   :9 :united-kingdom
   :10 :hong-kong
   :12 :taiwan
   :13 :canada
   :14 :russia
   :15 :germany})

(defn keys->countries
  "Accepts a hashmap of google trend data keyed by country number. Converts it into a
human readable format."
  [m]
  (let []
    (loop [ks (keys m)
           acc {}]
      (if (empty? ks)
        acc
        (let [k (first ks)]
          (recur (rest ks)
                 (assoc acc (google-mapping k) (m k))))))))

(defn google-trends []
  (keys->countries (raw-google-trends)))
