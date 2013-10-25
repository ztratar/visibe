(ns ^{:doc "..."}
  visibe.feeds.instagram
  (:use instagram.oauth
        ring.util.codec
        instagram.callbacks
        instagram.callbacks.handlers
        instagram.api.endpoint)
  (:require [visibe.state :refer [assoc-in-state! gis]]
            [visibe.feeds.storage :refer [append-datums]]
            [clj-time.coerce :refer [from-long]])
  (:import instagram.callbacks.protocols.SyncSingleCallback))

;;; TODO, Thu Oct 24 2013, Francis Wolke

;;; Use the pagination facilities to get more media

;;; We should prolly be storing all of the data and selectively querying against
;;; it. To prevent a loss of data, and keep it for future use, but it was easier
;;; to just discard it so I implemented that first.

;;; Handle video

(defn generate-oauth-creds! []
  (assoc-in-state! [:instagram :creds]
                   (make-oauth-creds (gis [:instagram :client-id])
                                     (gis [:instagram :client-secret])
                                     (gis [:instagram :redirect-url]))))

(defn trend->tag [trend]
  ;; XXX, Thu Oct 24 2013, Francis Wolke
  ;; Currently discarding all but one tag.
  (let [trend (clojure.string/replace trend " " "")]
    (:name (first (:data (:body (search-tags :oauth (gis [:instagram :creds]) :params {:q (url-encode trend)})))))))

(defn instagram-media
  "Accepts a trend, converts it to a tag name, searches instagram and returns
relevent media."
  [trend]
  (:data (:body (get-tagged-medias :oauth (gis [:instagram :creds])
                                   :params {:tag_name (trend->tag trend)}))))

(defn store-instagram-media
  "Instagram returns their datums with the newest first"
  [trend datums]
  (let [
        ;; XXX, Thu Oct 24 2013, Francis Wolke
        ;; UNIX time is in seconds, whereas java time is in milliseconds. To fix this,
        ;; we multiply by 1000.
        datums (map (fn [d] (update-in d [:created_time]
                                       #(str (from-long (* 1000 (read-string %)))))) datums)]
    (append-datums trend datums)))

(defn track-trend
  "Tracks a trend while it's still an active trend, persisting data related to
it."
  ;; NOTE, Thu Oct 24 2013, Francis Wolke
  ;; This trend tracking functionality could be pulled out into a marco.
  [trend]
  (future (loop [media #{}]
            (when (some #{trend} (keys (gis [:google :trends])))
              (let [new-media-q (instagram-media trend)
                    new-datums (clojure.set/difference (set media) (set new-media-q))]
                (store-instagram-media trend new-datums)
                (Thread/sleep 180000)
                (recur new-media-q))))))
