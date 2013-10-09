(ns ^{:doc "Coordination of different feeds"}
  visibe.feeds
  (:require [visibe.feeds.twitter :as twitter]
            [visibe.feeds.google-trends :as goog]))

(defn scrape-trends!
  "Scrapes trends, updates `state' but does not persist the data"
  []
  (future (loop [trends (:united-states (keys->countries (google-trends)))]
            ;; 5 min
            (Thread/sleep 300000)
            (recur (let [data (:united-states (keys->countries (google-trends)))]
                     (when-not (= trends data)
                       ;; Anything new trends that appear, track them.
                       (do ;; (doseq [new-trend (clojure.set/difference (set data) (set trends))]
                         ;;   (track-trend new-trend))
                         (update-state! [:app :trends] data)))
                     data)))))

(defn scrape-and-persist-trends!
  "Scrapes trends, updates `state' and perists the data when it changes"
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

(defn dev! []
  (scrape-trends!))

(defn production! []
  (scrape-and-persist-trends!))
