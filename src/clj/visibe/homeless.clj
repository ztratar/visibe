(ns ^{:doc "The place where all lost vars end up."}
  visibe.homeless
  (:require [visibe.schema :refer [tweet]]
            [visibe.rpc :refer [rpc-call]]
            [org.httpkit.server :as hk]))

(defn logic [s]
  (str (let [f (first (read-string s))]
         (cond (= 'open-stream f)
               "open-stream"
               #_(loop []
                   (do (Thread/sleep 30000) ; 30 sec
                       (tweet)
                       (recur)))
               (= 'close-stream f) "close stream"
               :else "other" #_(rpc-call s)))))

(defn test-handle [channel data]
  (let [f (first (read-string data))]
    (cond (= 'open-stream f)
          ;; This might have to run a future because it appears to be blocking
          ;; the thread.
          (loop []
            (do (Thread/sleep (/ 30000 30)) ; 1 sec
                (hk/send! channel "tweet" false)
                (recur)))
          (= 'close-stream f) (hk/send! channel "close stream" false)
          :else (hk/send! channel "else" false))))


