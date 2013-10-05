(ns ^{:doc "For collection of google trends data."}
  visibe.feeds.google-trends  
  (:require [clj-http.lite.client :as client]
            [visibe.storage :refer [persist-trends]]
            [visibe.core :refer [update-state!]]
            ;; TODO, Fri Oct 04 2013, Francis Wolke
            ;; replace `decode' with `read-json'
            [cheshire.core :refer [decode]]))

(defn google-trends []
  ;; NOTE, Mon Sep 30 2013, Francis Wolke
  ;; For the time being we don't need phantom.js as they only update this when
  ;; google does, and don't appear to care that we're hitting their API.
  (decode (:body (client/get "http://hawttrends.appspot.com/api/terms/"))))

(def google-mapping
  ;; NOTE, Mon Sep 30 2013, Francis Wolke
  ;; I have no idea why they're ranked like this (and have missing keys). The
  ;; second version of the should be able to figure out countries itself.
  {"1" :united-states
   "3" :india
   "4" :japan
   "5" :singapore
   "6" :israel
   "8" :australia
   "9" :united-kingdom
   "10" :hong-kong
   "12" :taiwan
   "13" :canada
   "14" :russia
   "15" :germany})

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

(defn scrape-google-trends
  "Every five minutes, scrapes google trend data."
  []
  (future
    ;; FIXME, Fri Oct 04 2013, Francis Wolke
    ;; I'm being lazy right now, and not dealing with data from other countries
    ;; until it we've got the system working from end to end.
    (loop [trends (:united-states (keys->countries (google-trends)))]
      ;; 5 min
      (Thread/sleep 300000)
      (recur (let [data (google-trends)]
               (when-not (= trends data)
                 (update-state! [:app :trends] data)
                 ;; (persist-trends data)
                 (:united-states (keys->countries (google-trends)))))))))

(defn dev-scrape-trends []
  ;; NOTE, Sat Oct 05 2013, Francis Wolke
  ;; I'll add in a 'dev mode' so this sort of stuff does not show up in the
  ;; codebase.
  (future (loop [trends (:united-states (keys->countries (google-trends)))]
            ;; 1 min
            (Thread/sleep (/ 300000 5))
            (recur (let [data (:united-states (keys->countries (google-trends)))]
                     (when-not (= trends data)
                       (update-state! [:app :trends] data))
                     data)))))
