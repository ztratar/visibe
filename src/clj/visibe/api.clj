(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [org.httpkit.server :as hk]
            [visibe.state :refer [state]]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]))

; Boilerplate
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

; WebSockets
;*******************************************************************************

;; (defn create-channel!
;;   "Creates a new channel"
;;   [channel]
;;   (defn open-new-channel [channel]
;;   (swap! channels (fn [atom-val] (assoc atom-val channel {:stream-open? false})))))

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
;;           :else (hk/send! channel (rpc-call data) false))))


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
;;   (defn open-stream [channel trend]
;;     (swap! channels (fn [atom-val] (assoc-in atom-val [channel :stream-open?] true)))
;;     (future (loop []
;;               (let [ks (select-keys (@channels channel) [:stream-open? :encoding])]
;;                 (case ks
;;                   {:encoding :edn :stream-open? true} (do (Thread/sleep (/ 30000 30)) ; 1 sec 
;;                                                           (hk/send! channel (str (tweet)) false)
;;                                                           (recur))
;;                   {:encoding :json :stream-open? true} (do (Thread/sleep (/ 30000 30)) ; 1 sec 
;;                                                            (hk/send! channel (encode (tweet)) false)
;;                                                            (recur))
;;                   nil)))))
;;   )

;; (defn close-stream!
;;   "Stops streaming data on the current trend to the specified channel"
;;   [channel trend]
;;   ;; (defn close-stream! [channel]
;;   ;;   (swap! channels (fn [atom-val] (assoc-in atom-val [channel :stream-open?] false))))
;;   )

(defn current-trends
  "Returns current google trends for specified region"
  []
  (get-in @state [:google :trends]))

(defn regions
  "Returns trending regions"
  []
  "regions"
  #_(vals (google-mapping)))

(defn previous-50-datums
  "Returns the 50 last (sorted chronologically)"
  [id]
  "previous-50-datums")

(defroutes api-routes
  (expose current-trends)
  (expose regions)
  (expose previous-50-datums))
