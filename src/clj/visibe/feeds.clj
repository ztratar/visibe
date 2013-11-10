(ns ^{:doc "Coordination of different feeds"}
  visibe.feeds
  (:require [clojure.set :as set]
            [clj-http.lite.client :as client]
            [visibe.feeds.instagram :as instagram]
            [visibe.feeds.twitter :as twitter]
            [visibe.feeds.flickr :as flickr]
            [image-resizer.crop :refer :all]
            [image-resizer.core :refer :all]
            [image-resizer.format :as format]
            [visibe.feeds.storage :refer [create-trend persist-google-trends-and-photos]]
            [visibe.state :refer [assoc-in-state! state]]
            [visibe.feeds.google-trends :as goog])
  (:import java.net.URL
           java.io.File
           javax.imageio.ImageIO))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn img-url->thumbnail-path
  ;; TODO, Sun Nov 10 2013, Francis Wolke
  ;; Center the cropping.
  "Downloads the appropriate image, then creates a cropped file for it on our
local server, returning the path to the file. The cropped file is a square,
where it's sides are the length of the shortest side of the supplied file."
  [url]
  (let [img (ImageIO/read (URL. url))
        [w h] [(.getWidth img) (.getHeight img)]]
    (when-not (= w h)
      (let [cropped-img (if (> w h)
                          ((crop-fn 0 0 h h) img)  ; horizontially
                          ((crop-fn 0 0 w w) img)) ; vertically
            u (uuid)]
        (format/as-file cropped-img (str "resources/public/cropped-images/" u ".png"))
        u))))

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

(defn trends->trends-with-photo-information
  "trends => {trend {:thumb URL-1 :full URL-2}}"
  [trends]
  (into {} (mapv (fn [trend]
                   (let [flickr-url (flickr/trend->photo-url trend)]
                     [trend {:full flickr-url
                             :thumb (img-url->thumbnail-path flickr-url)}])) trends)))

(defn scrape-and-persist-trends!
  "Main loop that starts all trend related data gathering. For each API other
that google-trends we have a `track-trend' function that runs in it's own thread
(a future) and persists it's own data. Google trend data is persisted in this
loop."
  ;; TODO, FIXME, Fri Oct 04 2013, Francis Wolke
  
  ;; I'm being lazy right now, and not dealing with data from other countries
  ;; until it we've got the system working from end to end.

  ;; use `future-cancel' here and in trend-tracking. It might be worthwile to
  ;; store a pointer to all launched futures so that I can kill them at the
  ;; REPL.
  []
  (future
    (loop [trends #{}]
      (recur (let [new-trends (:united-states (goog/google-trends))
                   new-trends (trends->trends-with-photo-information new-trends)]
               ;; persist the new hashmap of trends and their photos
               (persist-google-trends-and-photos new-trends)
               (when (not= trends new-trends)
                 (assoc-in-state! [:google :trends] new-trends) ; Google trends and associated flickr images
                 ;; Track trends on other social media sites
                 (let [new-diff-trends (set/difference (keys new-trends) (keys trends))]
                   (doseq [t new-diff-trends]
                     (create-trend t)
                     (twitter/track-trend t)
                     (instagram/track-trend t)))
                 
                 (do (Thread/sleep 300000) ; 5 min
                     new-trends)))))))
