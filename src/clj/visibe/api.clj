(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [org.httpkit.server :as hk]
            [visibe.state :refer [state assoc-in-state! update-in-state!]]
            [visibe.feeds.google-trends :refer [google-mapping]]
            [visibe.schemas :refer [n-sorted-tweets]]
            [visibe.feeds.storage :refer [previous-50-datums after-datum]]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]))

; WebSockets API
;*******************************************************************************

(defn ^{:api :websocket :doc "Sends generated test data instead of whatever."}
  toggle-stream!
  [channel]
  (update-in-state! [:app :channels channel :on] #(not %))
  {:on (get-in @state [:app :channels channel :on])})

(defn ^{:api :websocket :doc "Sends generated test data instead of whatever."}
  toggle-test-mode!
  [channel]
  (update-in-state! [:app :channels channel :test-mode] #(not %))
  {:test-mode (get-in @state [:app :channels channel :test-mode])})

(defn ^{:api :websocket :doc "Adds a new trend stream to a channel."}
  add-trend-stream!
  [channel trend]
  (update-in-state! [:app :channels channel :trends] (fn [& args] (set (apply conj args))) trend)
  {:trends (get-in @state [:app :channels channel :trends])})

(defn ^{:api :websocket :doc "Removes trend stream from a channel."}
  remove-trend-stream!
  [channel trend]
  (update-in-state! [:app :channels channel :trends] (fn [st] (set (remove #{trend} st))))
  {:trends (get-in @state [:app :channels channel :trends])})


; WebSockets Boilerplate
;*******************************************************************************

;;; Note, Sat Oct 12 2013, Francis Wolke

;;; Make detailed notes about how this websocket scheme works so that it may be
;;; citiqued without others having to read the code. Additionally, this will
;;; allow me to more easily identify it's flaws.

;;; TODO, Sat Oct 12 2013, Francis Wolke
;;; Add client-side tests for API.

(defn ds->ws-message
  ;; TODO, Tue Oct 15 2013, Francis Wolke
  ;; Add validation for `:type'
  ([ds] (ds->ws-message :print ds)) 
  ([type ds] (str {:type type :data ds})))

(defn establish-stream-loop!
  "Provides a live data stream on trends in ':trends' of a channel's context. Terminates with the channel."
  [channel]
  (future
    ;; TODO, Mon Oct 14 2013, Francis Wolke
    ;; Move unsure computation out into it's own function and rename.
    (loop [last-sent-datums (partition 2 (interleave (get-in @state [:app :channels channel :trends]) (repeat nil)))]
      (let [channel-context (get-in @state [:app :channels channel])]
        (cond (not (:on channel-context)) (do (Thread/sleep (/ 60000 60))
                                              (recur last-sent-datums))
              ;; Test Mode
              (:test-mode channel-context) (do (hk/send! channel (ds->ws-message :datums (n-sorted-tweets 5)))
                                               (Thread/sleep (/ 60000 60))
                                               (recur last-sent-datums))
              ;; Production
              :else (do (let [dts (map (fn [[tnd l-datum]] [tnd (after-datum tnd l-datum)])
                                       last-sent-datums)
                              ;; Remove trends without new data
                              dts (remove (fn [[_ d]] (nil? d)) dts)
                              to-recur (map (fn [[tnd datums]] [tnd (last datums)]) dts)]
                          
                          (hk/send! channel
                                    (ds->ws-message :datums (partition 2 (interleave (get-in @state [:app :channels channel :trends]) ;
                                                                                     (repeat (n-sorted-tweets 5))))))
                          ;; one minute
                          (Thread/sleep (/ 60000 60))
                          (recur to-recur))))))))

(defn register-new-channel!
  "Adds new channel and associated context to `state'"
  [channel]
  ;; `and' is used to guarantee transaction succeeds before esablishing a channel's loop.

  ;; TODO, Sat Oct 12 2013, Francis Wolke
  ;; I doubt that identifying a client by a websocket connection is a good
  ;; idea. Modify so we use some sort of UUID scheme. Also, ask aound on IRC.
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
                          (= fst 'toggle-stream!)       (toggle-stream! channel)
                          (= fst 'toggle-test-mode!)    (do (toggle-test-mode! channel)
                                                            (str {:test-mode (get-in @state [:app :channels channel :test-mode])}))
                          :else "Not a valid funtion. `help' and `doc' are not yet implemented."))))

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
