(ns ^{:doc "Coordination of different feeds"}
  visibe.feeds
  (:require [clojure.set :as set]
            [clj-http.lite.client :as client]
            [visibe.feeds.twitter :as twitter]
            [visibe.feeds.storage :refer [create-trend]]
            [visibe.state :refer [assoc-in-state! gis state]]
            [visibe.feeds.google-trends :as goog]))

;;; NOTE, Fri Oct 18 2013, Francis Wolke

;;; To get the best images, we could pay someone minimum wage to be the brain
;;; behind the image chooser. Whenever the trends update, they have to choose
;;; the images that best represent the trends. We just present them with a
;;; webpage that updates with the trends, and they select the images they find
;;; fits best.

(defn scrape-trends!
  "Scrapes trends, updates `state' but does not persist the data. Any datum feed
must be stubbed out."
  []
  (future (let [trends (:united-states (goog/google-trends))
                _ (assoc-in-state! [:google :trends] trends)]
            (loop [trends trends]
              ;; TODO, Sun Oct 13 2013, Francis Wolke
              ;; Inital data can be removed by moving `Thread/sleep' to `recur'
              ;; 5 min
              (Thread/sleep 300000)
              (recur (let [new-trends (:united-states (goog/google-trends))]
                       (when-not (= (set trends) (set new-trends))
                         (assoc-in-state! [:google :trends] new-trends))
                       new-trends))))))

(defn scrape-and-persist-trends!
  "Scrapes trends, updates `state' and perists the the data when it changes. Trends
are tracked via twitter, and relevent tweets are persisted via `twitter/track-trend'."
  []
  (future
    ;; FIXME, Fri Oct 04 2013, Francis Wolke
    ;; I'm being lazy right now, and not dealing with data from other countries
    ;; until it we've got the system working from end to end.
    (let [trends (:united-states (goog/google-trends))
          _ (assoc-in-state! [:google :trends] trends)
          _ (doseq [t trends]
              (create-trend t) 
              (twitter/track-trend t))]
      (loop [trends trends]
        ;; TODO, Sun Oct 13 2013, Francis Wolke
        ;; Inital data can be removed by moving `Thread/sleep' to `recur'
        ;; 5 min
        (Thread/sleep 300000)
        (recur (let [new-trends (:united-states (goog/google-trends))]
                 (when (not= (set trends) (set new-trends))
                   (assoc-in-state! [:google :trends] new-trends)
                   (let [new-diff-trends (set/difference (set new-trends) (set trends))]
                     (doseq [t new-diff-trends]
                       (create-trend t) 
                       (twitter/track-trend t)))
                   new-trends)))))))

(defn dev! []
  (scrape-trends!))

(defn production! []
  (scrape-and-persist-trends!))
