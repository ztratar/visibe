(ns ^{:doc "Conceptual start of the program."}
  eve.core
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [cljs.reader :as r]
            [shodan.console :as console]
            [eve.views :as v :refer [feed-update!]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [eve.state :refer [state assoc-in-state! update-in-state!]]
            [eve.templates :as templates]
            [cljs.core.async :as async :refer [<! >! chan put! timeout close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [dommy.macros :as m]))

;; http://marcopolo.io/2013/10/01/servant-cljs.html

; Misc
;*******************************************************************************

(repl/connect "http://localhost:8002/repl")

; Websockets
;*******************************************************************************

(def send (chan))
(def receive (chan))

(defn establish-ws-event-loop! []
  (go
   (while true
     (let [data (<! receive)
           msg (r/read-string (str (.-data data)))]
       (case (:type msg)
         ;; NOTE, Sun Nov 10 2013, Francis Wolke
         ;; These two are conceptually the same, shouldn't the code reflect that?
         :seed-datums (update-in-state! [:datums] (partial into (:data msg)))
         :datums (update-in-state! [:datums] (partial into (:data msg)))

         :current-trends (assoc-in-state! [:trends] (:data msg))
         :print (console/log (:data msg))
         :else (console/log (:data msg)))))))

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

(defn datum-count []
  (count (:datums @state)))

; Bootstrap
;*******************************************************************************

(defn bootstrap! []
  (set! (-> js/videojs (.-options) (.-flash) (.-swf)) "js/video-js/video-js.swf")
  (ws-connect!)
  (let [ch (chan)]
    (go (loop [v (:trends @state)]
          (>! ch v)
          (when (empty? v)
            (recur (:trends @state)))))
    (go (loop []
          (let [v (<! ch)]
            (if (empty? v)
              (recur)
              (do (v/navigate! :home)
                  (close! ch))))))))

(def on-load (set! (.-onload js/window) bootstrap!))
