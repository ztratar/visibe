(ns eve.core
  (:require-macros [dommy.macros :refer [sel sel1]])
  (:require [clojure.browser.repl :as repl]
            [dommy.utils :as utils]
            [dommy.core :refer [set-text! append!]]))

(repl/connect "http://localhost:8002/repl")

(defn printc [& m]
  (.log js/console (apply str m)))

(defn process-socket-data [data]
  (printc "!")
  (printc "msg: " (.-data data)))

(defn ws-t []
  (let [ws (js/WebSocket. "ws://localhost:3000/ws")
        _ (set! (.-onerror ws) #(printc "Websocket Error: " %))
        _ (set! (.-onmessage ws) process-socket-data)]
    ws))
