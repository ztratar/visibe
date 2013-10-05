(ns ^{:doc "Data generation and validation"}
  visibe.schema
  (:require [clj-schema.example :refer :all]
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

(def-map-schema tweet-schema
  ;; TODO, Fri Oct 04 2013, Francis Wolke
  ;; :foo_bar -> :foo-bar
  [[:text] String
   [:user] String
   [:name] String
   [:screen_name] String
   [:profile_image_url_https] String    ; should be URI. Also, is this optional?
   ])

(def-example-factory tweet tweet-schema []
  {:text  (random-str 140)
   :user  (random-str 10)
   :name  (random-str 10)
   :screen_name  (random-str 10)
   :profile_image_url_https (random-str 100)})
