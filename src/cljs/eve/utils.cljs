(ns eve.utils)

;;; (.table js/console "some tabular info")

(defn datum-count
  ([] (count (:datums @eve.state/state)))
  ([trend] (count (filter #(= trend (:trend %)) (:datums @eve.state/state)))))

(defn ->slug [s]
  (.toLowerCase (clojure.string/replace s " " "-")))

(defn slug->trend [slug]
  (ffirst (filter (fn [[n _]] (= slug (->slug n))) (into [] (:trends @eve.state/state)))))
