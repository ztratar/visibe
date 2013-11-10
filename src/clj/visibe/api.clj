(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [org.httpkit.server :as hk]
            [visibe.state :refer [state assoc-in-state! update-in-state! gis]]
            [visibe.feeds.google-trends :refer [google-mapping]]
            [visibe.schemas :refer [n-sorted-datums n-sorted-tweets]]
            [visibe.feeds.storage :refer [previous-50-datums after-datum intial-trend-datums]]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]))

; WebSockets API
;*******************************************************************************

(defn ^{:api :websocket :doc "Sends generated test data instead of whatever"}
  toggle-stream!
  [channel]
  (update-in-state! [:app :channels channel :on] #(not %))
  {:on (get-in @state [:app :channels channel :on])})

(defn ^{:api :websocket :doc "If in test mode streaming will send test data"}
  toggle-test-mode!
  [channel]
  (update-in-state! [:app :channels channel :test-mode] #(not %))
  {:test-mode (get-in @state [:app :channels channel :test-mode])})

(defn ^{:api :websocket :doc "Adds a new trend stream to a channel"}
  add-trend-stream!
  [channel trend]
  (update-in-state! [:app :channels channel :trends] (fn [& args] (set (apply conj args))) trend)
  {:trends (get-in @state [:app :channels channel :trends])})

(defn ^{:api :websocket :doc "Removes trend stream from a channel"}
  remove-trend-stream!
  [channel trend]
  (update-in-state! [:app :channels channel :trends] (fn [st] (set (remove #{trend} st))))
  {:trends (get-in @state [:app :channels channel :trends])})

(defn help ^{:api :websocket :doc "Returns information about the API for consumtion by human, or near human intelligences"}
  []
  (apply str (cons "The funtions you have avalible to you are:\n"
                   (interleave (repeat "\n")
                               ["toggle-stream!"
                                "toggle-test-mode!"
                                "add-trend-stream!"
                                "remove-trend-stream!"
                                "help"
                                "\nI regret to inform you that we do not have `doc' implemented yet."]))))


; WebSockets Boilerplate
;*******************************************************************************

;;; TODO, Sun Nov 10 2013, Francis Wolke
;;; A community clojurescript websocket implementation exists. Use that instead.

(defn ds->ws-message
  ([ds] (ds->ws-message :print ds)) 
  ([type ds] (str {:type type :data ds})))

(defn establish-stream-loop!
  "Provides a live data stream on trends in ':trends' of a channel's context. Terminates with the channel."
  [channel]
  (future
    (loop [google-trends {}
           last-sent-datums (partition 2 (interleave (get-in @state [:app :channels channel :trends]) (repeat nil)))]
      (let [channel-context (get-in @state [:app :channels channel])
            new-google-trends (gis [:google :trends])]
        ;; Should we send new trends data?
        (when-not (= new-google-trends google-trends)
          (hk/send! channel (ds->ws-message :new-trends new-google-trends)))

        ;; If this is the first call, supply started data
        (when (= {} google-trends)
          (intial-trend-datums (keys new-google-trends)))

        (cond (not (:on channel-context)) (do (Thread/sleep (/ 60000 60))
                                              (recur last-sent-datums
                                                     (gis [:google :trends])))
              ;; Test Mode
              (:test-mode channel-context) (do (hk/send! channel (ds->ws-message :datums (n-sorted-datums 5)))
                                               (Thread/sleep (/ 60000 60))
                                               (recur last-sent-datums
                                                      (gis [:google :trends])))
              ;; Production
              :else (do (let [dts (map (fn [[tnd l-datum]] [tnd (after-datum tnd l-datum)])
                                       last-sent-datums)
                              ;; Remove trends without new data
                              dts (remove (fn [[_ d]] (nil? d)) dts)
                              to-recur (map (fn [[tnd datums]] [tnd (last datums)]) dts)]
                              
                          (hk/send! channel
                                    (ds->ws-message :datums (partition 2 (interleave (get-in @state [:app :channels channel :trends])
                                                                                     (repeat (n-sorted-tweets 5))))))
                          ;; one minute
                          (Thread/sleep (/ 60000 60))
                          (recur to-recur
                                 (gis [:google :trends])))))))))

(defn register-new-channel!
  "Adds new channel and associated context to `state'"
  [channel]
  ;; `and' is used to guarantee transaction succeeds before esablishing a channel's loop.
  (and (assoc-in-state! [:app :channels channel] {:trends #{} :test-mode false :on false})
       (establish-stream-loop! channel)))

(defn destroy-channel! [channel]
  (update-in-state! [:app :channels] dissoc channel))

(defn ws-api-call [channel data]
  ;; TODO, Sun Oct 13 2013, Francis Wolke
  ;; Don't pass back all the data about `state' atom when we're in production
  ;; Check arglists.
  
  (when-not (get-in @state [:app :channels channel])
    (register-new-channel! channel))
  (let [ds (read-string data)
        fst (first ds)]
    (ds->ws-message (cond (= fst 'add-trend-stream!)    (add-trend-stream! channel (second ds))
                          (= fst 'remove-trend-stream!) (remove-trend-stream! channel (second ds))
                          (= fst 'help)                 (help)
                          (= fst 'toggle-stream!)       (toggle-stream! channel)
                          (= fst 'toggle-test-mode!)    (do (toggle-test-mode! channel)
                                                            (str {:test-mode (get-in @state [:app :channels channel :test-mode])}))
                          :else "Try `help'. `doc' is not yet implemented."))))

(defn websocket-handler
  [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [& _] (destroy-channel! channel)))
    (hk/on-receive channel (fn [data] (hk/send! channel (ws-api-call channel data))))))

; HTTP Boilerplate
;*******************************************************************************

(defn fn-name->route [sym]
  (clojure.string/replace (str sym) "-" "/"))

(defmacro expose [sym]
  `(~'POST ~(str "/api/" (fn-name->route sym))
           {~'body :body}
           (str (let [~'body (when ~'body (read-string (slurp ~'body)))]
                  (if ~'body
                    (apply ~sym ~'body)
                    (~sym))))))

; HTTP
;*******************************************************************************

(defn current-trends
  "Returns current google trends for specified region along with their
associated flickr urls"
  []
  (get-in @state [:google :trends]))

(defn regions
  "Returns trending regions"
  []
  (vals google-mapping))

(defroutes api-routes
  (expose current-trends)
  (expose regions)
  (expose previous-50-datums))
