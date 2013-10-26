(ns ^{:doc "Data generation and validation"}
  visibe.schemas
  (:require [clj-schema.example :refer :all]
            [clj-time.core :refer [date-time]]
            [visibe.homeless :refer [sort-datums-by-timestamp]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [clj-schema.schema :refer :all]
            [clj-schema.validation :refer :all]))

;;; TODO, Thu Oct 24 2013, Francis Wolke

;;; Use faker for test data generation

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
   [:created-at] String
   [:name] String
   [:screen-name] String
   [:type] keyword
   ;; TODO, Fri Oct 04 2013, Francis Wolke
   ;; should be URI. Also, is this optional?
   [:profile-image-url-https] String])

(def-map-schema instagram-media-schema []
  
  )

(def-map-schema instagram-photo-schema
  [[:created_time] String
   [:type] :instagram-photo
   [:full_name] String 
   [:id] String
   [:link] String
   [:photo-url] String
   [:profile-picture] String
   [:tags] (sequence-of string?)
   [:username] String])

(def-map-schema instagram-video-schema
  [[:created-at] String
   [:full-name] String
   [:id] String
   [:link] String
   [:poster-image-url] String
   [:profile-picture] String
   [:tags] (sequence-of string?)
   [:username] String
   [:video-url] String])

; Factories 
;*******************************************************************************

(def-example-factory tweet tweet-schema []
  {:text  (random-str 140)
   :type :tweet
   :user  (random-str 10)
   :created-at (str (random-date-time))
   :name  (random-str 10)
   :screen-name  (random-str 10)
   :profile-image-url-https
   "https://si0.twimg.com/profile_images/2622165696/o20xkpll5fr57alshtnd_normal.jpeg"})

(def-example-factory tweet instagram-photo-schema []
  {:text  (random-str 140)
   :type :instagram-photo
   :user  (random-str 10)
   :created-at (str (random-date-time))
   :name  (random-str 10)
   :screen-name  (random-str 10)
   :profile-image-url-https
   "https://si0.twimg.com/profile_images/2622165696/o20xkpll5fr57alshtnd_normal.jpeg"})

(def-example-factory tweet instagram-video-schema []
  {:text  (random-str 140)
   :type :tweet
   :user  (random-str 10)
   :created-at (str (random-date-time))
   :name  (random-str 10)
   :screen-name  (random-str 10)
   :profile-image-url-https "https://si0.twimg.com/profile_images/2622165696/o20xkpll5fr57alshtnd_normal.jpeg"})

; Utils
;*******************************************************************************

;;; TODO, Thu Oct 24 2013, Francis Wolke
;;; Nothing here is being sorted

(defn n-sorted-tweets
  "n tweets, sorted by timestamp, oldest first"
  [n]
  (sort-datums-by-timestamp (take n (iterate (fn [_] (tweet)) (tweet)))))

;; (defn n-sorted-instagrams
;;   "n instagrams, sorted by timestamp, oldest first"
;;   [n]
;;   )

(defn n-sorted-datums
  "n datums, sorted by timestamp, oldest first"
  [n]
  )
