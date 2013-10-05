(ns ^{:doc "Data generation and validation"}
  visibe.schemas
  (:require [clj-time.core :refer [date-time]]
            [clj-schema.example :refer :all]
            [clj-schema.schema :refer :all]
            [clj-schema.validation :refer :all]))

;;; String generation.

(def valid-chars
  (map char (concat (range 66 91)    
                    (range 97 123))))

(defn random-char []
  (nth valid-chars (rand (count valid-chars))))

(defn random-str [length]
  (apply str (take length (repeatedly random-char))))

(defn random-date-time []
  (date-time 2013 10 (rand-int 30) (rand-int 24) (rand-int 60)))

(def-map-schema tweet-schema
  ;; TODO, Fri Oct 04 2013, Francis Wolke
  
  ;; :foo_bar -> :foo-bar

  ;; I'm ignoring time-zones for the time being
  [[:text] String
   [:user] String
   [:created_at] String
   [:name] String
   [:screen_name] String
   ;; TODO, Fri Oct 04 2013, Francis Wolke
   ;; should be URI. Also, is this optional?
   [:profile_image_url_https] String])

(def-example-factory tweet tweet-schema []
  {:text  (random-str 140)
   :user  (random-str 10)
   :created_at (str (random-date-time))
   :name  (random-str 10)
   :screen_name  (random-str 10)
   :profile_image_url_https "https://si0.twimg.com/profile_images/2622165696/o20xkpll5fr57alshtnd_normal.jpeg"})
