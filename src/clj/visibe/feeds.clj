(ns ^{:doc "Social feed loops"}
  visibe.feeds  
  (:require [clojure.set :as set]
            [visibe.homeless :refer [sleep]]
            [org.httpkit.server :as hk]
            [visibe.api :refer [ds->ws-message]]
            [clj-http.lite.client :as client]
            [visibe.feeds.instagram :as instagram]
            [visibe.feeds.twitter :as twitter]
            [visibe.feeds.flickr :refer [trend->photo-url]]
            [visibe.feeds.storage :refer [persist-google-trends-and-photos youngest-trends]]
            [visibe.state :refer [assoc-in-state! state gis]]
            [visibe.feeds.google-trends :as goog])
  (:import java.net.URL
           java.io.ByteArrayOutputStream
           java.io.ByteArrayInputStream
           java.io.File
           javax.imageio.ImageIO))

(defn subscribed-clients
  "Seq of websocket channels for clients subscribed to TREND"
  [trend]
  (let [channels (seq (gis [:app :channels]))
        subscribed-and-on? (fn [[channel context]]
                             (and (some #{trend} (:subscriptions context)) (:on context)))]
    (map first (filter subscribed-and-on? channels))))

(defn push-datums-to-subscribed-clients!
  "Sends DATUMS on websocket channels subscribed to TREND"
  [trend datums]
  (let [subscribed-clients (subscribed-clients trend)]
    (when-not (empty? subscribed-clients) 
      (doseq [client subscribed-clients]
        (hk/send! client (ds->ws-message :datums datums))))))

(defn active?
  "Is a this trend a 'current trend', or subscribed to by a client?"
  [trend]
  (or (some #{trend} (keys (gis [:google :trends])))
      (not (empty? (subscribed-clients trend)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Trend tracking

(defn instagram-track-trend
  "Tracks a trend while it's an active trend, or susbscribed to by a client
   persisting data related to it and pushing data to subscribed clients."
  [trend]
  (future (loop [media #{}]
            (when (active? trend)
              (let [new-media  (instagram/instagram-media trend)
                    new-datums (clojure.set/difference (set new-media) media)]
                (future (push-datums-to-subscribed-clients! trend new-datums))
                (instagram/store-instagram-media trend new-datums)
                (sleep 1)
                (recur new-datums))))))

(defn twitter-track-trend
  "Tracks a trend while it's still an 'active' trend. Runs in future, persisting
   data and pushing it to clients when appropriate"
  ;; NOTE, Fri Oct 04 2013, Francis Wolke
  ;; For the time being, I don't want to deal with the full stream.
  ;; Twitter's rate limit window is 15 minutes. We are allowed 450 requests over
  ;; this peiod of time. (/ (* 15 60) 450) => 2 sec
  [trend]
  (future (loop [tweet-data (twitter/search-tweets trend)]
            (when (active? trend)
              (future (push-datums-to-subscribed-clients! trend (:statuses tweet-data)))
              ;; Naked append datums call?
              (twitter/store-tweets trend tweet-data)
              (sleep 1)
              (recur (twitter/next-page (-> tweet-data :search_metadata :refresh_url)))))))

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
    (loop [trends {}]
      (recur (let [new-trends (into {} (vec (pmap (fn [url] [url (trend->photo-url url)])
                                                  (:united-states (goog/google-trends)))))]

               (persist-google-trends-and-photos new-trends)
               
               (when (not= trends new-trends)

                 (future (doseq [client (seq (gis [:app :channels]))]
                           (hk/send! client (:current-trends new-trends))))
                 
                 (let [difference (set/difference (keys new-trends) (keys trends))]
                   (assoc-in-state! [:google :trends] (into {} (mapv (fn [x] [x (new-trends x)]) difference)))
                   (doseq [t difference]
                     (twitter-track-trend t)
                     (instagram-track-trend t)))
                 
                 (do (sleep 5)
                     new-trends)))))))

