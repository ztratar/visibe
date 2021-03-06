(ns visibe.feeds
  "Social feed loops"
  (:require [clj-http.lite.client       :as    client]
            [clojure.set                :as    set]
            [org.httpkit.server         :as    hk]
            [visibe.api                 :refer [ds->ws-message]]
            [visibe.feeds.flickr        :refer [trend->photo-url]]
            [visibe.feeds.google-trends :refer [google-trends trend->goog-photo-url]]
            [visibe.feeds.instagram     :as    instagram]
            [visibe.feeds.sanitation    :refer :all]
            [visibe.feeds.storage       :refer [persist-google-trends-and-photos append-datums popular-trends]]
            [visibe.feeds.twitter       :as    twitter]
            [visibe.homeless            :refer [sleep]]
            [visibe.state               :refer [assoc-in-state! state gis]])
  (:import java.net.URL
           java.io.ByteArrayOutputStream
           java.io.ByteArrayInputStream
           java.io.File
           javax.imageio.ImageIO))

(defn- subscribed-clients
  "Seq of websocket channels for clients subscribed to TREND"
  [trend]
  (let [channels (seq (gis [:app :channels]))
        subscribed-and-on? (fn [[channel context]]
                             (and (some #{trend} (:subscriptions context)) (:on context)))]
    (map first (filter subscribed-and-on? channels))))

(defn- push-datums-to-subscribed-clients!
  "Sends DATUMS on websocket channels subscribed to TREND"
  [trend datums]
  (let [subscribed-clients (subscribed-clients trend)]
    (when-not (empty? subscribed-clients) 
      (doseq [client subscribed-clients]
        (hk/send! client (ds->ws-message :datums (map datum->essentials datums)))))))

(defn- active?
  "Is a this trend a 'current trend', or subscribed to by a client?"
  [trend]
  (or (some #{trend} (keys (gis [:google :trends])))
      (not (empty? (subscribed-clients trend)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Trend tracking

;;; FIXME, Tue Dec 10 2013, Francis Wolke
;;; It's confusing to have a seprate `clean-X' process that occurs before
;;; storing the datums. It needs its own function independant of the 'tracking'

(defn instagram-track-trend
  "Tracks a trend while it's an active trend, or susbscribed to by a client
   persisting data related to it and pushing data to subscribed clients."
  ;; TODO, Sat Nov 23 2013, Francis Wolke
  ;; Currently does not gather all the data that we could possibly gather
  [trend]
  (future (loop [media #{}]
            (when (active? trend)
              (let [new-datums (set (map (partial clean-instagram trend)
                                         (instagram/instagram-media trend)))
                    new-datums (set/difference new-datums media)]
                (future (push-datums-to-subscribed-clients! trend new-datums))
                (append-datums trend new-datums)
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
  (future (loop [twitter-data (twitter/search-tweets trend)]
            (let [clean-tweets (map (partial clean-tweet trend) (:statuses twitter-data))]
              (when (active? trend)
                ;; Only send clients relevent data
                (future (push-datums-to-subscribed-clients! trend clean-tweets))
                (append-datums trend clean-tweets)
                (sleep 1)
                (recur (twitter/next-page (-> twitter-data :search_metadata :refresh_url))))))))

(defn- trends-blob
  [trends]
  (into {} (vec (pmap (fn [url] [url (trend->goog-photo-url url)]) trends))))

(defn scrape-and-persist-trends!
  "Main loop that initiates all trend related data gathering. For each API other
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
  (future (loop [trends {}]
            (recur (let [new-trends (trends-blob (google-trends))]

                     (persist-google-trends-and-photos new-trends)
                     (when (not= trends new-trends)
                       (future (doseq [client (seq (gis [:app :channels]))]
                                 (hk/send! client (:current-trends new-trends))))

                       (let [difference (set/difference (keys new-trends) (keys trends))]
                         (assoc-in-state! [:google :trends] (trends-blob difference))
                         (doseq [t difference]
                           (twitter-track-trend t)
                           (instagram-track-trend t))))

                     (sleep 5)
                     new-trends)))))

(defn popular-trends!
  "Populates `state' with some of the most popular trends. For use when you do
   not wish to spin up the number of threads consumed by the running application."
  []
  (assoc-in-state! [:google :trends] (trends-blob (popular-trends))))
