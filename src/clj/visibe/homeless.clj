(ns visibe.homeless
  "Place where lost vars end up"
  (:require [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-time.core :refer [date-time]]))

(defn sleep [n-minutes]
  (Thread/sleep (* n-minutes 60000)))

(defn date-time-str->long [s]
  (to-long (f/parse (f/formatters :date-time) s)))

(defn rfc822-str->long 
  [s]
  (to-long (f/parse (f/formatters :rfc822) s)))
