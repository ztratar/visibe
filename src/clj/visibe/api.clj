(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [org.httpkit.server :as hk]
            [visibe.state :refer [state assoc-in-state! update-in-state! gis]]
            [visibe.feeds.google-trends :refer [google-mapping]]
            [visibe.feeds.storage :as storage]
            [visibe.feeds.twitter :refer [tweet->essentials]]
            [visibe.feeds.instagram :refer [instagram-photo->essentials instagram-video->essentials]]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]))

;;; TODO, Wed Nov 13 2013, Francis Wolke
;;; Dynamic `help' and `doc'
;;; A community websocket implementation exists. Use that instead. http://cljwamp.us/

; WebSockets API
;*******************************************************************************

(defn ^{:api :websocket :doc "Returns current google trends for specified region along with their associated flickr urls"}
  current-trends
  []
  (gis [:google :trends]))

(defn ^{:api :websocket :doc "Returns your current context"}
  channel-context [channel]
  (gis [:app :channels channel]))

;; (defn ^{:api :websocket :doc "..."}
;;   previous-15
;;   [channel {created-at :created-at trend :trend}]
;;   (hk/send! channel (ds->ws-message))
;;   {:subscriptions (gis [:app :channels channel :subscriptions])})

(defn ^{:api :websocket :doc "If `:off' we don't send test data or production data"}
  toggle-stream!
  [channel]
  (update-in-state! [:app :channels channel :on] not)
  {:on (gis [:app :channels channel :on])})

(defn ^{:api :websocket :doc "Subscribes you to a particular trend stream, you will receive datums after DATUM"}
  subscribe!
  [channel {created-at :created-at trend :trend}]
  (update-in-state! [:app :channels channel :subscriptions]
                    (fn [subscriptions] (conj subscriptions [trend created-at])))
  {:subscriptions (gis [:app :channels channel :subscriptions])})

(defn ^{:api :websocket :doc "You will no longer be sent datums related to this trend."}
  unsubscribe!
  [channel trend-to-remove]
  (update-in-state! [:app :channels channel :subscriptions]
                    (fn [subscriptions] (remove (fn [[t _]] (= trend-to-remove t)) subscriptions)))
  {:subscriptions (gis [:app :channels channel :subscriptions])})

; WebSockets Boilerplate
;*******************************************************************************

(defn ds->ws-message
  "[d]ata [s]tructure -> websocket message"
  ([ds] (ds->ws-message :print ds)) 
  ([type ds] (str {:type type :data ds})))

(defn clean-datum [datum]
  (cond (= (:datum-type datum) "instagram-photo") (instagram-photo->essentials datum)
        (= (:datum-type datum) "instagram-video") (instagram-video->essentials datum)
        (= (:datum-type datum) "tweet") (tweet->essentials datum)
        :else datum))

(defn- sleep "sleeps for one minute" [] (Thread/sleep (/ 60000 60)))

(defn establish-stream-loop!
  "Provides a live data stream on trends in ':trends' of a channel's context. Terminates with the channel."
  [channel]
  (future
    (loop [trends {}]
      (let [channel-context (gis [:app :channels channel])
            new-trends (keys (gis [:google :trends]))]

        ;; Send seed data?
        (when (= {} trends)          
          (hk/send! channel (ds->ws-message :seed-datums (map clean-datum (reduce into (map storage/seed-datums new-trends))))))
        
        (when-not (nil? channel-context)
          (if (:on channel-context)
            (let [subscriptions (:subscriptions channel-context)
                  ;; hold onto the trend information so that it may be used later to update subscriptions
                  new-datums-and-subs-info (map (fn [[trend created-at]] [trend (storage/datums-since trend created-at)]) subscriptions)
                  datums-packaged-to-send (map clean-datum (reduce into (map second new-datums-and-subs-info)))]
              
              ;; XXX, NOTE Wed Nov 13 2013, Francis Wolke
              ;; If a client `unsubscribes!' from a trend inbetween the time it takes to read out and update the subscriptions, we will end up
              ;; leaving them subscribed to that trend.

              (future (let [updated-subscriptions (map (fn [[trend datums]] [trend (:created-at (first datums))]) new-datums-and-subs-info)]
                        (assoc-in-state! [:app :channels channel :subscriptions] updated-subscriptions)))

              (hk/send! channel (ds->ws-message :datums datums-packaged-to-send))
              
              (sleep)
              (recur new-trends))

            (do (sleep) (recur new-trends))))))))

(defn help ^{:api :websocket :doc "Returns information about the API for consumtion by human, or near human intelligences"}
  []
  (apply str (cons "The funtions you have avalible to you are NOTE THAT THIS IS NOT UP TO DATE:\n"
                   (interleave (repeat "\n")
                               ["subscribe!"
                                "channel-context"
                                "unsubscribe!"
                                "current-trends"
                                "help"
                                "\nI regret to inform you that we do not have `doc' implemented yet."]))))

(defn register-new-channel!
  "Adds new channel and associated context to `state'"
  [channel]
  ;; `and' is used to guarantee transaction finishes before esablishing a channel's loop.
  (and (assoc-in-state! [:app :channels channel] {:subscriptions [] :on false})
       (establish-stream-loop! channel)))

(defn destroy-channel! [channel]
  (update-in-state! [:app :channels] dissoc channel))

(defn ws-api-call [channel data]
  (when-not (get-in @state [:app :channels channel])
    (register-new-channel! channel))

  (let [ds (read-string data)
        fst (first ds)]
    (cond (= fst 'subscribe!)      (ds->ws-message (subscribe! channel (second ds)))
          (= fst 'unsubscribe!)    (ds->ws-message (unsubscribe! channel (second ds)))
          (= fst 'channel-context) (ds->ws-message (channel-context channel))
          (= fst 'current-trends)  (ds->ws-message :current-trends (current-trends))
          (= fst 'toggle-stream!)  (ds->ws-message (toggle-stream! channel))
          (= fst 'previous-15)     (ds->ws-message "Needs to be wired up")
          (= fst 'help)            (ds->ws-message (help))
          :else "Try `help'. `doc' is not yet implemented.")))

(defn websocket-handler
  [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [& _] (destroy-channel! channel)))
    (hk/on-receive channel (fn [data] (hk/send! channel (ws-api-call channel data))))))
