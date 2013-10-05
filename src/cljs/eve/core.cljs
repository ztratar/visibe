(ns eve.core
  (:require [clojure.browser.repl :as repl]))

(repl/connect "http://localhost:8002/repl")

(def conn)

(defn printc [& m]
  (.log js/console (apply str m)))

(defn process-socket-data [data]
  ;; Add some predicates to this where we check what data comes across.
  (printc (.-data data)))

(defn ws-connect []
  (let [ws (js/WebSocket. "ws://localhost:3000/ws")
        _ (set! (.-onerror ws) #(printc "Websocket Error: " %))
        _ (set! (.-onmessage ws) process-socket-data)]
    (def conn ws)))

(defn rpc-call [s]
  (.send conn s))

;;; Call these at the REPL.

;; (ws-connect)

;; (rpc-call "(open-stream)")
;; (rpc-call "(close-stream)")
;; (rpc-call "(doc FUNCTION)")
;; (rpc-call "(trends :united-states)")
;; (rpc-call "(help)")

;;; Each of these would then print something out to the REPL. The important ones here being `open-stream' and `close stream'. The idea is to:

;; a) Fix the broken code. Which will yield a general API to work with that allows me to expose a few powerful functions that can yeild all possibe data you could use on the frontend. Then, you can 

;; b) call (trends REGION) to get a list of trends, use it to update the splash page.

;; c) Call (open-stream REGION) to begin streaming 'real-time' datums over websockets and produce the 'live feed' effect.

;; c) Call (close-stream REGION) to stop streaming 'real-time' datums over websocket connection.

;;; The other functions are there for utility.
