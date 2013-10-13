(ns ^{:doc "Application state. In it's own `ns' to prevent circular
  dependencies."}
  visibe.state)

;;; NOTE, Tue Oct 08 2013, Francis Wolke
;;; Expect the names, structure and info associated with `state' to change
;;; often.

;;; TODO, Wed Oct 02 2013, Francis Wolke
;;; Use clj-schema to verify config file is correct.

;;; Check `state' on every update. - allow changes that wern't supposed to
;;; happen persist, but notify me.

;; Verify that broken connections are not hanging around

(def state (atom {:mongo {:username nil
                          :password nil
                          :host nil
                          :database nil
                          :port nil}
 
                  :twitter {:access-token nil
                            :access-token-secret nil	
                            :consumer-key nil
                            :consumer-secret nil
                            :bearer-token nil}
                  
                  :app {:port nil
                        :nrepl-port nil
                        :server nil
                        :nrepl-server nil
                        :channels {}}

                  :google {:trends []}}))

(defn update-state! [path form]
  ;; TODO, Wed Oct 09 2013, Francis Wolke
  ;; Remove
  (swap! state assoc-in path form))

(defn assoc-in-state! [path form]
  (swap! state assoc-in path form))

(defn read-config!
  "Associates information from a specified file with `state'"
  []
  ;; XXX, Tue Oct 08 2013, Francis Wolke
  ;; Dependent on the current structure of the config file
  (let [data (read-string (slurp "config.cljd"))
        vs (vals data)
        kvs (map keys vs)
        vvs (map vals vs)
        vs (map (fn [[f l]] (interleave f l)) (partition 2 (interleave kvs vvs)))
        data (partition 2 (interleave (keys data) vs))
        data (loop [d data
                    acc []]
               (if (empty? d)
                 acc
                 (let [h (partition 2 (second (first d)))]
                   (recur (rest d)
                          (conj acc (map #(cons (ffirst d) %) h))))))
        data (map #(list (vec (take 2 %)) (last %)) (reduce into data))]
    (doseq [[f l] data]
      (update-state! f l))))
