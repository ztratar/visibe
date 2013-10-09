(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [visibe.feeds.google-trends :refer [google-trends]]
            [org.httpkit.server :as hk]
            [visibe.state :refer [state]]))

;;; TODO, Tue Oct 08 2013, Francis Wolke

;;; Create a `defn-api-fn' macro that creates a route for the function. It
;;; should be aware of the data encoding specified by the client.

; Boilerplate
;*******************************************************************************

;; (def ^{:doc "Consumed by X which creates doc functions and `api-routes'."}
;;   http-fns)

;; (defmacro def-api-fn [name docstring args & body])

;; (defn create-channel!
;;   "Creates a new channel"
;;   [channel]
;;   ;; (defn open-new-channel [channel]
;;   ;; (swap! channels (fn [atom-val] (assoc atom-val channel {:stream-open? false
;;   ;;                                                         :encoding :json}))))
;;   )

(defn destroy-channel!
  "Destroys a channel"
  [channel])

;; (defn test-handle [channel data]
;;   (when-not (@channels channel)
;;     (open-new-channel channel))
;;   (let [d (read-string data)
;;         f (first d)
;;         r (rest d)]
;;     (cond (= 'open-stream f) (open-stream channel data)
;;           (= 'close-stream f) (do (close-stream! channel)
;;                                   (hk/send! channel "Stream closed" false))
;;           (= 'toggle-stream-encoding! f) (toggle-stream-encoding! channel)
;;           :else (hk/send! channel (rpc-call data) false))))


; WebSockets
;*******************************************************************************

;;; It dosn't make any sense to have funtion calls made over websockets. HTTP
;;; Will work for that, and then we can have websockets stream the data. This
;;; will prevent any sort of duality of the API where it has to decide what
;;; protocol to use

(defn websocket-handler
  [request]
  (hk/with-channel request channel
    ;; Remove me, and replace with something more appropriate.
    ;; (swap! state update-in [:app :channels] conj channel)
    (hk/on-close channel (fn [_] (destroy-channel! channel)))

    ;; TODO, Tue Oct 08 2013, Francis Wolke
    ;; If this is a new channel, then `create-channel!' in any other case
    ;; We should never reciving data from the client. Send to dev/null
    (hk/on-receive channel (fn [data] (hk/send! channel (str "ECHO:" data))))))

; HTTP
;*******************************************************************************

;; (defn open-stream!
;;   "Begins streaming real time data to the client on the specifed trend."
;;   ;; TODO, Tue Oct 08 2013, Francis Wolke
;;   ;; Where will we get the channel info from? 
;;   [channel trend]
;;   ;; (defn open-stream [channel trend]
;;   ;; (swap! channels (fn [atom-val] (assoc-in atom-val [channel :stream-open?] true)))
;;   ;; (future (loop []
;;   ;;           (let [ks (select-keys (@channels channel) [:stream-open? :encoding])]
;;   ;;             (case ks
;;   ;;               {:encoding :edn :stream-open? true} (do (Thread/sleep (/ 30000 30)) ; 1 sec 
;;   ;;                                                       (hk/send! channel (str (tweet)) false)
;;   ;;                                                       (recur))
;;   ;;               {:encoding :json :stream-open? true} (do (Thread/sleep (/ 30000 30)) ; 1 sec 
;;   ;;                                                        (hk/send! channel (encode (tweet)) false)
;;   ;;                                                        (recur))
;;   ;;               nil)))))
;;   )

;; (defn close-stream!
;;   "Stops streaming data on the current trend to the specified channel"
;;   [channel trend]
;;   ;; (defn close-stream! [channel]
;;   ;;   (swap! channels (fn [atom-val] (assoc-in atom-val [channel :stream-open?] false))))
;;   )

;; (defn toggle-encoding!
;;   "Toggles between JSON or EDN as encoding for data returned from the API"
;;   []
;;   ;; (defn toggle-stream-encoding! [channel]
;;   ;; (swap! channels (fn [atom-val] (let [encoding (:encoding (atom-val channel))]
;;   ;;                                  (assoc-in atom-val [channel :encoding]
;;   ;;                                            (if (= encoding :json) :edn :json))))))
;;   )

;; (defn current-trends
;;   "Returns current google trends for specified region"
;;   []
;;   (get-in @state [:google :trends]))

;; (defn regions
;;   "Returns trending regions"
;;   []
;;   (vals (google-mapping)))

;; (defn previous-50-datums
;;   "Returns the 50 last (sorted chronologically)"
;;   [id]
;;   )

;; (defn doc
;;   "Returns documentation associated with a symbol that is part of the API, HTTP
;; or websockets."
;;   ;; TODO, Tue Oct 08 2013, Francis Wolke
;;   ;; Try via HTTP and WS to see which feels more natural
;;   [sym]
;;   )
