(ns ^{:doc "The place where all lost vars end up."}
  visibe.homeless
  (:require [visibe.schema :refer [tweet]]
            [visibe.rpc :refer [rpc-call]]
            [org.httpkit.server :as hk]))

(def channels (atom {}))

(defn open-stream [channel s]
  (swap! channels (fn [atom-val]
                    (assoc-in atom-val [channel :stream-open?] true)))
  (future (loop []
            (if (:stream-open? (@channels channel))
              (do (Thread/sleep (/ 30000 30)) ; 1 sec
                  (hk/send! channel "tweet" false)
                  (recur))
              nil))))

(defn close-stream! [channel]
  (swap! channels (fn [atom-val] (assoc-in atom-val [channel :stream-open?] false))))

(defn open-new-channel [channel]
  (swap! channels (fn [atom-val] (assoc atom-val channel {:stream-open? false}))))

(defn test-handle [channel data]
  (when-not (@channels channel)
    (open-new-channel channel))
  (let [f (first (read-string data))]
    (cond (= 'open-stream f) (open-stream channel data)
          (= 'close-stream f) (do (close-stream! channel)
                                  (hk/send! channel "Stream closed" false))
          :else (hk/send! channel (rpc-call data) false))))
