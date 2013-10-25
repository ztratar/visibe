(ns eve.core
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [cljs.reader :as r]
            [shodan.console :as console]
            [eve.views :as v]
            [eve.views.trend :refer [feed-update!]]
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

;; (def d3 js/d3)

; Websockets
;*******************************************************************************

(defn process-socket-data [data]
  (let [msg (r/read-string (str (.-data data)))]
    (case (:type msg)
      :datums (update-in-state! [:datums] (partial into (:data msg)))
      :print (console/log (:data msg))
      :else (console/log (:data msg)))))

(defn ws-connect! []
  (let [ws (js/WebSocket. "ws://localhost:9000/ws")
        _ (set! (.-onerror ws) #(console/error "WebSocket: " %))
        _ (set! (.-onmessage ws) process-socket-data)]
    (assoc-in-state! [:websocket-connection] ws)))

(defn wsc [f]
  (if-let [conn (:websocket-connection @state)]
    (.send conn (str f))
    (console/error "You must establish a WebSocket connection"))) 

(defn update-current-trends! []
  (go (let [response (<! (http/post "http://localhost:9000/api/current/trends"
                                    {:headers {"content-type" "application/data"}}))]
        (assoc-in-state! [:trends] (r/read-string (:body response)))))) 

(defn route->fn-name [sym]
  (clojure.string/replace (str sym) "/" "-"))

; Bootstrap
;*******************************************************************************

(defn ^:export bootstrap! []
  (update-current-trends!)
  (ws-connect!)
  (add-watch state :feed feed-update)
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
