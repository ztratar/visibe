(ns eve.ws ^{:doc "Websockets"}
    (:require [cljs.core.async :as async :refer [<! >! chan put! timeout close!]]
              [cljs.reader :as r]
              [eve.state :refer [update-in-state! assoc-in-state! state]]
              [shodan.console :as console])
    (:require-macros [cljs.core.async.macros :refer [go alt!]]))

(def send (chan))
(def receive (chan))

(defn establish-ws-event-loop! []
  (go
   (while true
     (let [data (<! receive)
           msg (r/read-string (str (.-data data)))]
       (case (:type msg)
         :datums         (update-in-state! [:datums] (comp set (partial into (:data msg))))
         :current-trends (do (console/log "got current trends")
                             (assoc-in-state! [:trends] (:data msg)))
         :print          (console/log (str (:data msg)))
         (console/log "ws data" (str (:data msg))))))))

(defn ws-connect! []
  (let [ws (js/WebSocket. "ws://localhost:9000/ws")
        _ (set! (.-onerror ws) #(console/error "WebSocket: " %))
        _ (set! (.-onmessage ws) (fn [msg] (put! receive msg)))
        _ (set! (.-onopen ws) (fn [& _] (.send ws "(current-trends)")))]
    (assoc-in-state! [:websocket-connection] ws)
    (establish-ws-event-loop!)))

(defn wsc [f]
  (if-let [conn (:websocket-connection @state)]
    (.send conn (str f))
    (console/error "You must establish a WebSocket connection"))) 
