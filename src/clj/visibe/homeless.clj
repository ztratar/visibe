(ns ^{:doc "Place where lost vars end up"}
  visibe.homeless
  (:require [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-time.core :refer [date-time]]))

(defn date-time-str->long [s]
  (to-long (f/parse (f/formatters :date-time) s)))

(defn rfc822-str->long
  "Tue, 08 Oct 2013 04:08:48 +0000 => 1381205328000"
  [s]
  (to-long (f/parse (f/formatters :rfc822) s)))
