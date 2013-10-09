(ns ^{:doc "Place where lost vars end up"}
  visibe.homeless
  (:require [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-time.core :refer [date-time]]))

(defn date-time-str->long [s]
  (to-long (f/parse (f/formatters :date-time) s)))

(defn sort-datums-by-timestamp
  "Sorts datums by timstamp, oldest first"
  [datums]
  (sort-by #(date-time-str->long (:created-at %)) datums))

