(ns ^{:doc "Coordination of different feeds"}
  visibe.feeds
  (:require [clojure.set :as set]
            [clj-http.lite.client :as client]
            [visibe.feeds.twitter :as twitter]
            [visibe.feeds.flickr :as flickr]
            [visibe.feeds.storage :refer [create-trend persist-google-trends-and-photos]]
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
  (future (loop [trends (:united-states (goog/google-trends))]
            (recur (let [new-trends (:united-states (goog/google-trends))]
                     (when-not (= (set trends) (set new-trends))
                       (assoc-in-state! [:google :trends] new-trends))
                     (Thread/sleep 300000) ; 5 min
                     new-trends)))))

(defn scrape-and-persist-trends!
  "Main loop that starts all trend related data gathering. For each API other that google-trends we have a `track-trend' function that runs in it's own thread (a future) and persists it's own data. Google trend data is persisted in this loop."
  ;; TODO, FIXME, Fri Oct 04 2013, Francis Wolke
  
  ;; I'm being lazy right now, and not dealing with data from other countries
  ;; until it we've got the system working from end to end.

  ;; use `future-cancel' here and in trend-tracking. It might be worthwile to store a pointer to all launched futures so that I can kill them at the REPL.
  []
  (future
    (loop [trends #{}]
      (recur (let [new-trends (:united-states (goog/google-trends))
                   trends-and-photo-urls (mapv (fn [trend] [trend (flickr/trend->photo-url trend)]) new-trends)
                   new-trends (into {} trends-and-photo-urls)]
               ;; persist the new hashmap of trends and their photos
               (persist-google-trends-and-photos new-trends)
               (when (not= trends new-trends)
                 (assoc-in-state! [:google :trends] new-trends) ; Google trends and associated flickr images
                 ;; Twitter
                 (let [new-diff-trends (set/difference (keys new-trends) (keys trends))]
                   (doseq [t new-diff-trends]
                     (create-trend t)
                     (twitter/track-trend t)
                     ;; (instagram/track-trend t)
                     ))
                 
                 (do (Thread/sleep 300000) ; 5 min
                     new-trends)))))))
