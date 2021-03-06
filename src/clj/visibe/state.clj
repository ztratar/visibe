(ns visibe.state
  "Application state. In it's own `ns' to prevent circular
  dependencies.")

;;; TODO, Thu Oct 24 2013, Francis Wolke
;;; Check `state' on every update against a schema. Allow all transactions to
;;; persist, but notify me if something unexpected happens.

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

                  :flickr {:key nil
                           :secret nil}
                  
                  :app {:port nil
                        :nrepl-port nil
                        :server nil
                        :nrepl-server nil
                        :channels {}}

                  :google {:trends []}}))

(defn update-in-state!
  ([path f] (swap! state update-in path f))
  ([path f x] (swap! state update-in path f x)))

(defn assoc-in-state! [path form]
  (swap! state assoc-in path form))

(defn gis
  "[g]et [i]n [s]tate"
  [path]
  (get-in @state path))

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
      (assoc-in-state! f l))))

(defn ^{:dev true} stop-server []
  ((get-in @state [:app :server])))
