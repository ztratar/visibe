(ns ^{:doc "Coordination of different feeds"}
  visibe.feeds
  (:require [clojure.set :as set]
            [visibe.feeds.twitter :as twitter]
            [visibe.feeds.storage :refer [create-trend]]
            [visibe.state :refer [update-state!]]
            [visibe.feeds.google-trends :as goog]))

(defn scrape-trends!
  "Scrapes trends, updates `state' but does not persist the data. Any datum feed
must be stubbed out."
  []
  (future (loop [trends (:united-states (goog/google-trends))]
            ;; 5 min
            (Thread/sleep 300000)
            (recur (let [new-trends (:united-states (goog/google-trends))]
                     (when-not (= trends new-trends)
                       (update-state! [:app :trends] new-trends))
                     new-trends)))))

(defn scrape-and-persist-trends!
  "Scrapes trends, updates `state' and perists the data when it changes. Trends
are tracked on twitter, and relevent tweets are persisted."
  []
  (future
    ;; FIXME, Fri Oct 04 2013, Francis Wolke
    ;; I'm being lazy right now, and not dealing with data from other countries
    ;; until it we've got the system working from end to end.
    (loop [trends (:united-states (goog/google-trends))]
      ;; 5 min
      (Thread/sleep 300000)
      (recur (let [new-trends (:united-states (goog/google-trends))]
               (when (not= trends new-trends)
                 ;; TODO, Wed Oct 09 2013, Francis Wolke
                 ;; Rename.
                 (let [new-diff-trends (set/difference (set new-trends) (set trends))]
                   (doseq [t new-diff-trends]
                     (create-trend t) 
                     (twitter/track-trend t)))
                 (update-state! [:app :trends] new-trends)
                 new-trends))))))

(defn dev! []
  (scrape-trends!))

(defn production! []
  (scrape-and-persist-trends!))
