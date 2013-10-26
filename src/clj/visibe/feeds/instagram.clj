(ns ^{:doc "..."}
  visibe.feeds.instagram
  (:use instagram.oauth
        ring.util.codec
        instagram.callbacks
        instagram.callbacks.handlers
        instagram.api.endpoint)
  (:require [visibe.state :refer [assoc-in-state! gis]]
            [visibe.feeds.storage :refer [append-datums]]
            [clj-time.coerce :refer [from-long]]
            [clojure.set :refer [rename-keys]])
  (:import instagram.callbacks.protocols.SyncSingleCallback))

(defn generate-oauth-creds! []
  (assoc-in-state! [:instagram :creds]
                   (make-oauth-creds (gis [:instagram :client-id])
                                     (gis [:instagram :client-secret])
                                     (gis [:instagram :redirect-url]))))

(defn- trend->tag [trend]
  ;; XXX, Thu Oct 24 2013, Francis Wolke
  ;; Currently discarding all but one of the returned tag.
  (let [trend (clojure.string/replace trend " " "")]
    (:name (first (:data (:body (search-tags :oauth (gis [:instagram :creds]) :params {:q (url-encode trend)})))))))

(defn instagram-media
  "Accepts a trend, converts it to a tag name, searches instagram and returns
relevent media."
  [trend]
  ;; FIXME, Thu Oct 24 2013, Francis Wolke
  ;; Currently we are not grabbing all of the data that we could be. Make use of
  ;; the pagination feature.
  (:data (:body (get-tagged-medias :oauth (gis [:instagram :creds])
                                   :params {:tag_name (trend->tag trend)}))))

(defn store-instagram-media
  "Instagram returns their datums with the newest first"
  [trend datums]
  (let [
        ;; XXX, Thu Oct 24 2013, Francis Wolke
        ;; UNIX time is in seconds, whereas java time is in milliseconds. To compenstate,
        ;; we multiply by 1000.
        datums (map (fn [d] (update-in d [:created_time]
                                       #(str (from-long (* 1000 (read-string %)))))) datums)]
    (append-datums trend datums)))

(defn- instagram-photo->datum [m]
  (let [a (partial get-in m)]
    (merge (select-keys m [:tags :id :created_time :link])
           {:full-name (a [:user :full_name])
            :profile-picture (a [:user :profile_picture])
            :username (a [:user :username])
            :photo (a [:images :standard_resolution])
            :type :instagram-photo})))

(defn- instagram-video->datum [m]
  (let []
    
    ))

(defn- instagram-media->datum
  "Accepts and instagram media map and returns it's essential constituents."
  [m]
  (if (= "image" (:type m))
    (instagram-photo->essentials m)
    (instagram-video->essentials m)))

(defn track-trend
  "Tracks a trend while it's still an active trend, persisting data related to
it."
  [trend]
  (future (loop [media #{}]
            (when (some #{trend} (keys (gis [:google :trends])))
              (let [new-media-q (instagram-media trend)
                    new-datums (clojure.set/difference (set media) (set new-media-q))]
                (store-instagram-media trend new-datums)
                (Thread/sleep 180000)
                (recur new-media-q))))))


