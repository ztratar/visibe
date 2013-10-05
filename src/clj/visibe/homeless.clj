(ns ^{:doc "The place where all lost vars end up."}
  visibe.homeless
  (:require [visibe.schema :refer [tweet]]
            [visibe.rpc :refer [rpc-call]]
            [org.httpkit.server :as hk]))

(defn rpc-handler [channel data]
  (hk/send! channel "foo" false)
  #_(let [f (first (read-string data))]
    (if (= 'start-stream f)
      (loop []
        (do (Thread/sleep 30000)        ; 30 sec
            (hk/send! channel (str (tweet)) false)
            (recur)))
      (rpc-call data))))
