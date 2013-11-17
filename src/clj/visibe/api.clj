(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [org.httpkit.server :as hk]
            [visibe.state :refer [state assoc-in-state! update-in-state! gis]]
            [visibe.feeds.google-trends :refer [google-mapping]]
            [visibe.feeds.storage :as storage]
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

(defn ^{:api :websocket :doc "If `:off' we don't send test data or production data"}
  toggle-stream!
  [channel]
  (update-in-state! [:app :channels channel :on] not)
  {:on (gis [:app :channels channel :on])})

(defn ^{:api :websocket :doc "Subscribes you to a particular trend stream, you will receive datums after DATUM"}
  subscribe!
  [channel trend]
  (update-in-state! [:app :channels channel :subscriptions]
                    (fn [subscriptions] (conj subscriptions trend)))
  {:subscriptions (gis [:app :channels channel :subscriptions])})

(defn ^{:api :websocket :doc "You will no longer be sent datums related to this trend."}
  unsubscribe!
  [channel trend]
  (update-in-state! [:app :channels channel :subscriptions]
                    (fn [subscriptions] (set (remove #{trend} subscriptions))))
  {:subscriptions (gis [:app :channels channel :subscriptions])})

; WebSockets Boilerplate
;*******************************************************************************

(defn ds->ws-message
  "[d]ata [s]tructure -> websocket message"
  ([ds] (ds->ws-message :print ds)) 
  ([type ds] (str {:type type :data ds})))

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
  ;; TODO, FIXME Sun Nov 17 2013, Francis Wolke
  ;; We are sending the `current-trends' twice.
  (future (doseq [trend (keys (gis [:google :trends]))]
            (hk/send! channel (ds->ws-message :datums (storage/seed-datums trend)))))
  (assoc-in-state! [:app :channels channel] {:subscriptions #{} :on true}))

(defn destroy-channel! [channel]
  (update-in-state! [:app :channels] dissoc channel))

(defn ws-api-call [channel data]
  (when-not (get-in @state [:app :channels channel])
    (future (hk/send! channel (ds->ws-message :current-trends (current-trends))))
    (register-new-channel! channel))

  (let [ds (read-string data)
        fst (first ds)]
    (cond (= fst 'subscribe!)      (ds->ws-message (subscribe! channel (second ds)))
          (= fst 'unsubscribe!)    (ds->ws-message (unsubscribe! channel (second ds)))
          (= fst 'channel-context) (ds->ws-message (channel-context channel))
          (= fst 'current-trends)  (ds->ws-message :current-trends (current-trends))
          (= fst 'toggle-stream!)  (ds->ws-message (toggle-stream! channel))
          (= fst 'previous-15)     (ds->ws-message :datums (storage/previous-15 (second ds)))
          (= fst 'help)            (ds->ws-message (help))
          :else "Try `help'. `doc' is not yet implemented.")))

(defn websocket-handler
  [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [& _] (destroy-channel! channel)))
    (hk/on-receive channel (fn [data] (hk/send! channel (ws-api-call channel data))))))
