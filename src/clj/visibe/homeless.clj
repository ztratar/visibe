(ns ^{:doc "The place where all lost vars end up."}
  visibe.homeless
  (:use user)
  (:require [visibe.schemas :refer [tweet]]
            [visibe.rpc :refer [rpc-call]]
            [cheshire.core :refer [encode]]
            [org.httpkit.server :as hk]))

(def channels (atom {}))

(defn open-stream [channel trend]
  (swap! channels (fn [atom-val] (assoc-in atom-val [channel :stream-open?] true)))
  (future (loop []
            (let [ks (select-keys (@channels channel) [:stream-open? :encoding])]
              (case ks
                {:encoding :edn :stream-open? true} (do (Thread/sleep (/ 30000 30)) ; 1 sec 
                                                        (hk/send! channel (str (tweet)) false)
                                                        (recur))
                {:encoding :json :stream-open? true} (do (Thread/sleep (/ 30000 30)) ; 1 sec 
                                                         (hk/send! channel (encode (tweet)) false)
                                                         (recur))
                nil)))))

(defn close-stream! [channel]
  (swap! channels (fn [atom-val] (assoc-in atom-val [channel :stream-open?] false))))

(defn open-new-channel [channel]
  (swap! channels (fn [atom-val] (assoc atom-val channel {:stream-open? false
                                                          :encoding :json}))))

(defn toggle-stream-encoding! [channel]
  (swap! channels (fn [atom-val] (let [encoding (:encoding (atom-val channel))]
                                   (assoc-in atom-val [channel :encoding]
                                             (if (= encoding :json) :edn :json))))))

(defn test-handle [channel data]
  (when-not (@channels channel)
    (open-new-channel channel))
  (let [d (read-string data)
        f (first d)
        r (rest d)]
    (cond (= 'open-stream f) (open-stream channel data)
          (= 'close-stream f) (do (close-stream! channel)
                                  (hk/send! channel "Stream closed" false))
          (= 'toggle-stream-encoding! f) (toggle-stream-encoding! channel)
          :else (hk/send! channel (rpc-call data) false))))
