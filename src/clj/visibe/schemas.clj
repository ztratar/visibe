(ns ^{:doc "Data generation and validation"}
  visibe.schemas
  (:use faker.name
        faker.lorem)
  (:require [clj-schema.example :refer :all]
            [clj-time.core :refer [date-time]] 
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-schema.schema :refer :all]
            [clj-schema.validation :refer :all]
            [faker.lorem :as fl]))

;;; TODO, Thu Oct 24 2013, Francis Wolke

;;; Unify these schemas

;;; Ensure that we are using the same time format internally

;;; Why not 'def' schemas in map form? EG: {:foo (wild String)}

;;; `scaffold-schema' needs to be a macro that checks types of the passed data
;;; structure, then produces the appropriate schema.

;;; I should be able to supply a map and a set of keys that I'd find useful, to
;;; a macro that then generates a schema for me, and a function that accepts one
;;; of those maps and turns it into the requested data structure. In this same
;;; mindset,I should be able to get all of, or at least most of an X factory
;;; spec from that data structure.

;;; Before spending a bunch of time on this, it would be useful to see what
;;; core.typed brings to the table.

; String generation.
;*******************************************************************************

(def valid-chars
  (map char (concat (range 66 91) (range 97 123))))

(defn random-char []
  (nth valid-chars (rand (count valid-chars))))

(defn random-str [length]
  (apply str (take length (repeatedly random-char))))

(defn random-date-time []
  (date-time 2013 10 (inc (rand-int 30)) (rand-int 24) (rand-int 60)))

; Schemas
;*******************************************************************************

(def-map-schema tweet-schema
  ;; FIXME, Fri Oct 04 2013, Francis Wolke

  ;; I'm ignoring time-zones for the time being
  [[:text] String
   [:user] String
   [:created-at] Long
   [:name] String
   [:screen-name] String
   [:type] keyword
   ;; TODO, Fri Oct 04 2013, Francis Wolke
   ;; should be URI. Also, is this optional?
   [:profile-image-url-https] String])

(def-map-schema instagram-media-schema []
  [[:url] String
   [:width] Number
   [:height] Number])

(def-map-schema instagram-photo-schema
  [[:type] :instagram-photo
   [:created-at] Long
   [:full-name] String 
   [:id] String
   [:link] String
   [:photo] instagram-media-schema
   [:profile-picture] String
   [:tags] (sequence-of string?)
   [:username] String])

(def-map-schema instagram-video-schema
  [[:type] :instagram-video
   [:created-at] Long
   [:full-name] String
   [:id] String
   [:link] String
   [:video] instagram-media-schema
   [:profile-picture] String
   [:tags] (sequence-of string?)
   [:username] String])

; Factories 
;*******************************************************************************

(def-example-factory tweet tweet-schema []
  {:text  (random-str 140)
   :type :tweet
   :user  (random-str 10)
   :created-at (to-long (random-date-time))
   :name  (random-str 10)
   :screen-name  (random-str 10)
   :profile-image-url-https
   "https://si0.twimg.com/profile_images/2622165696/o20xkpll5fr57alshtnd_normal.jpeg"})

(def-example-factory instagram-photo instagram-photo-schema []
  {:created-at (to-long (random-date-time))
   :type :instagram-photo
   :photo {:url "http://distilleryimage2.s3.amazonaws.com/dfcf86743e0f11e3880922000ae8030e_8.jpg"
           :width 640 :height 640}
   :username (str (first-name) (last-name))
   :profile-picture "http://images.ak.instagram.com/profiles/profile_29299173_75sq_1371200325.jpg"
   :full-name (first-name)
   :link "http://instagram.com/p/f7BN_IQTrK/"
   :id  (apply str (take 10 (repeatedly #(rand-int 42))))
   :tags (take 5 (words))})

(def-example-factory instagram-video instagram-video-schema []
  {:video {:width 640 :height 640
           :url "http://distilleryimage5.s3.amazonaws.com/087d432afb2d11e282f822000a1fbd33_101.mp4"}
   :link "http://instagram.com/p/cf2L-dx5h1/"
   :full-name (str (first-name) (last-name))
   :profile-picture "http://images.ak.instagram.com/profiles/profile_2119581_75sq_1358723016.jpg"
   :username (first-name)
   :type :instagram-video
   :created-at (to-long (random-date-time))
   :id (apply str (take 10 (repeatedly #(rand-int 42))))
   :tags (take 5 (words))})

; Utils
;*******************************************************************************

(defn n-tweets
  [n]
  (take n (repeatedly #(tweet))))

(defn n-instagrams
  [n]
  (take n (repeatedly #(if (zero? (rand-int 2)) (instagram-photo) (instagram-video)))))

(defn n-datums
  [n]
  (take n (set (into (n-sorted-tweets n) (n-sorted-instagrams n)))))
