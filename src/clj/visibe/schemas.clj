(ns ^{:doc "Data generation and validation"}
  visibe.schemas
  (:require [clj-schema.example :refer :all]
            [clj-time.core :refer [date-time]]
            [visibe.homeless :refer [sort-datums-by-timestamp]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-schema.schema :refer :all]
            [clj-schema.validation :refer :all]))

; String generation.
;*******************************************************************************

(def valid-chars
  (map char (concat (range 66 91)    
                    (range 97 123))))

(defn random-char []
  (nth valid-chars (rand (count valid-chars))))

(defn random-str [length]
  (apply str (take length (repeatedly random-char))))

(defn random-date-rfc822 [s]
  (to-long (f/parse (f/formatters :rfc822) s))
  (str (date-time 2013 10 (inc (rand-int 30)) (rand-int 24) (rand-int 60))))

; Schemas
;*******************************************************************************

(def-map-schema tweet-schema
  ;; TODO, Fri Oct 04 2013, Francis Wolke

  ;; I'm ignoring time-zones for the time being
  [[:text] String
   [:user] String
   [:created-at] String
   [:name] String
   [:screen-name] String
   ;; TODO, Fri Oct 04 2013, Francis Wolke
   ;; should be URI. Also, is this optional?
   [:profile-image-url-https] String])

(def-example-factory tweet tweet-schema
  []
  {:text  (random-str 140)
   :user  (random-str 10)
   :created-at (str (random-date-time))
   :name  (random-str 10)
   :screen-name  (random-str 10)
   :profile-image-url-https
   "https://si0.twimg.com/profile_images/2622165696/o20xkpll5fr57alshtnd_normal.jpeg"})

; Utils
;*******************************************************************************

(defn n-sorted-tweets
  "n tweets, sorted by timestamp, oldest first"
  [n]
  (sort-datums-by-timestamp (take n (iterate (fn [_] (tweet)) (tweet)))))
