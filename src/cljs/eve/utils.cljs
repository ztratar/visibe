(ns ^{:doc "..."}
  eve.utils)

(defn ->slug [s]
  (.toLowerCase (clojure.string/replace s " " "-")))
