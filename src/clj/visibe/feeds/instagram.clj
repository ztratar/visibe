(ns ^{:doc "Boilerplate for gathering trend data from instagram"}
  visibe.feeds.instagram
  (:use instagram.oauth
        ring.util.codec
        instagram.callbacks
        instagram.callbacks.handlers
        instagram.api.endpoint)
  (:require [visibe.state :refer [assoc-in-state! gis]]
            [org.httpkit.server :as hk]
            [visibe.feeds.storage :refer [append-datums]]
            [visibe.homeless :refer [rfc822-str->long]]
            [clj-time.coerce :refer [from-long]]
            [clojure.set :refer [rename-keys]])
  (:import instagram.callbacks.protocols.SyncSingleCallback))

(defn ds->ws-message
  "[d]ata [s]tructure -> websocket message"
  ([ds] (ds->ws-message :print ds)) 
  ([type ds] (str {:type type :data ds})))

(defn instagram-photo->essentials [m]
  (let [a (partial get-in m)]
    (-> m
        (select-keys [:tags :id :created-at :link :trend :datum-type])
        (merge {:full-name (a [:user :full_name])
                :profile-picture (a [:user :profile_picture])
                :username (a [:user :username])
                :photo (a [:images :standard_resolution])})
        (assoc :datum-type :instagram-photo))))

(defn instagram-video->essentials [m]
  (let [a (partial get-in m)]
    (-> m
        (select-keys [:tags :id :created-at :link :trend :datum-type])
        (merge {:full-name (a [:user :full_name])
                :profile-picture (a [:user :profile_picture])
                :username (a [:user :username])
                :video (a [:videos :standard_resolution])})
        (assoc :datum-type :instagram-video))))

(defn generate-oauth-creds! []
  (assoc-in-state! [:instagram :creds]
                   (make-oauth-creds (gis [:instagram :client-id])
                                     (gis [:instagram :client-secret])
                                     (gis [:instagram :redirect-url]))))

(defn- trend->tag [trend]
  ;; XXX, Thu Oct 24 2013, Francis Wolke
  ;; Currently discarding all but one of the returned tags.
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
  [trend datums]
  (letfn [(clean [datum]
            (-> (if (= "image" (:type datum))
                  (assoc datum :datum-type :instagram-photo)
                  (assoc datum :datum-type :instagram-video))
                (rename-keys {:created_time :created-at})
                ;; Java time is in millis, UNIX time (as returned by instagram) is in seconds
                (update-in [:created-at] (fn [t] (* 1000) (if (string? t) (Integer/parseInt t) t)))
                (assoc :trend trend)))]
    (append-datums trend (map clean datums))))

(defn clean-instagram [instagram]
  (if (= "image" (:type instagram))
    (instagram-photo->essentials instagram)
    (instagram-video->essentials instagram)))

(defn subscribed-clients [trend]
  (map first (filter (fn [[channel context]] (and (:on context) (some #{trend} (:subscriptions context)))) (seq (gis [:app :channels])))))

(defn push-instagrams-to-subscribed-clients! [trend instagrams]
  (let [clients (seq (gis [:app :channels]))
        subscribed-clients (when-not (empty? clients) (subscribed-clients trend))]

    (when-not (empty? subscribed-clients)
      (let [clean-instagrams (map #(assoc (clean-instagram %) :trend trend) instagrams)]
        (doseq [client subscribed-clients]
          (hk/send! client (ds->ws-message :datums clean-instagrams)))))))

(defn track-trend
  "Tracks a trend while it's still an active trend, persisting data related to it and pushing data to subscribed clients."
  [trend]
  (future (loop [media #{}]
            (when (some #{trend} (keys (gis [:google :trends])))
              (let [new-media  (instagram-media trend)
                    new-datums (clojure.set/difference (set new-media) media)]
                (future (push-instagrams-to-subscribed-clients! trend new-datums))
                (store-instagram-media trend new-datums)
                (Thread/sleep (/ 180000 3)) ; 1 minute
                (recur new-datums))))))
