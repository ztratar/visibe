(ns ^{:doc "..."}
  eve.utils
  (:require [eve.state :refer [state]]))

;;; (.table js/console "some tabular info")

(defn datum-count
  ([] (count (:datums @state)))
  ([trend] (count (filter #(= trend (:trend %)) (:datums @state)))))

(defn ->slug [s]
  (.toLowerCase (clojure.string/replace s " " "-")))
