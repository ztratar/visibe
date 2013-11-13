(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [org.httpkit.server :as hk]
            [visibe.state :refer [state assoc-in-state! update-in-state! gis]]
            [visibe.feeds.google-trends :refer [google-mapping]]
            [visibe.feeds.storage :refer [previous-50-datums after-datum intial-datums]]
            [visibe.feeds.twitter :refer [tweet->essentials]]
            [visibe.feeds.instagram :refer [instagram-photo->essentials instagram-video->essentials]]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]))

; WebSockets API
;*******************************************************************************

(defn current-trends ^{:api :websocket
                       :doc "Returns current google trends for specified region along with their associated flickr urls"}
  []
  (gis [:google :trends]))

(defn ^{:api :websocket :doc "If `:off' we don't send test data or production data"}
  toggle-stream!
  [channel]
  (update-in-state! [:app :channels channel :on] #(not %))
  {:on (get-in @state [:app :channels channel :on])})

(defn ^{:api :websocket :doc "Subscribes you to a particular trend stream, you will receive datums after DATUM"}
  subscribe!
  [channel datum]
  (update-in-state! [:app :channels channel :trends] (fn [trends] (set (conj trends datum))))
  {:trends (get-in @state [:app :channels channel :trends])})

(defn ^{:api :websocket :doc "You will no longer be sent datums related to this trend."}
  unsubscribe!
  [channel trend]
  (update-in-state! [:app :channels channel :trends] (fn [st] (set (remove #(= trend (:trend %)) st))))
  {:trends (get-in @state [:app :channels channel :trends])})

;;; TODO, Sun Nov 10 2013, Francis Wolke
;;; Make `help' work dynamically
(defn help ^{:api :websocket :doc "Returns information about the API for consumtion by human, or near human intelligences"}
  []
  (apply str (cons "The funtions you have avalible to you are NOTE THAT THIS IS NOT UP TO DATE:\n"
                   (interleave (repeat "\n")
                               ["subscribe!"
                                "unsubscribe!"
                                "current-trends"
                                "help"
                                "\nI regret to inform you that we do not have `doc' implemented yet."]))))

; WebSockets Boilerplate
;*******************************************************************************

;;; TODO, Sun Nov 10 2013, Francis Wolke
;;; A community websocket implementation exists. Use that instead.
;;; http://cljwamp.us/

(defn ds->ws-message
  "[d]ata [structure] -> websocket message"
  ([ds] (ds->ws-message :print ds)) 
  ([type ds] (str {:type type :data ds})))

(defn clean-datum [datum]
  (cond (= (:datum-type datum) "instagram-photo") (instagram-photo->essentials datum)
        (= (:datum-type datum) "instagram-video") (instagram-video->essentials datum)
        (= (:datum-type datum) "tweet") (tweet->essentials datum)
        :else datum))

(defn establish-stream-loop!
  "Provides a live data stream on trends in ':trends' of a channel's context. Terminates with the channel."
  [channel]
  (future
    (loop [google-trends {}]

      (let [channel-context (get-in @state [:app :channels channel])
            new-google-trends (gis [:google :trends])]

        ;; Send initial data
        (when (= {} google-trends)
          (hk/send! channel (ds->ws-message :seed-datums (map clean-datum (intial-datums (keys new-google-trends))))))

        (if (not (:on channel-context))

          ;; Stream is off
          (do (Thread/sleep (/ 60000 60))
              (recur new-google-trends))

          ;; Stream is on
          (do (let [subscriptions (:trends channel-context)
                    new-datums (map after-datum subscriptions)]
                
                ;; Update our subscriptions
                (assoc-in-state! [channel :trends] (map first new-datums))
                ;; Send the data
                (doseq [datum (reduce into new-datums)]
                  (hk/send! channel (ds->ws-message :datum (clean-datum datum))))
                
                (Thread/sleep (/ 60000 60)) ; one minute
                (recur new-google-trends))))))))

(defn register-new-channel!
  "Adds new channel and associated context to `state'"
  [channel]
  ;; `and' is used to guarantee transaction finishes before esablishing a channel's loop.
  (and (assoc-in-state! [:app :channels channel] {:trends #{} :on false})
       (establish-stream-loop! channel)))

(defn destroy-channel! [channel]
  (update-in-state! [:app :channels] dissoc channel))

(defn ws-api-call [channel data]
  (when-not (get-in @state [:app :channels channel])
    (register-new-channel! channel))

  (let [ds (read-string data)
        fst (first ds)]
    (cond (= fst 'subscribe!)     (ds->ws-message (subscribe! channel (second ds)))
          (= fst 'unsubscribe!)   (ds->ws-message (unsubscribe! channel (second ds)))
          (= fst 'current-trends) (ds->ws-message :current-trends (current-trends))
          (= fst 'help)           (ds->ws-message (help))
          (= fst 'toggle-stream!) (ds->ws-message (toggle-stream! channel))
          :else "Try `help'. `doc' is not yet implemented.")))

(defn websocket-handler
  [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [& _] (destroy-channel! channel)))
    (hk/on-receive channel (fn [data] (hk/send! channel (ws-api-call channel data))))))

;;;


;; (def play-data (intial-datums (keys (gis [:google :trends]))))

;; (:datum-type (first play-data))
