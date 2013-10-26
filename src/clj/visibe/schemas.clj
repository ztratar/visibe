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
;;; Ensure that we are using the same time format internally

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

(def-map-schema instagram-schema
  ;; NOTE, Thu Oct 24 2013, Francis Wolke
  ;; Why 'def' schemas like this and not in map form. EG: {:foo (wild String)}
  [[:attribution] (wild String)
   [:created_time] (wild String)
   [:filter] (wild String)
   [:id] (wild String)
   [:link] (wild String)
   [:location] (wild String)
   [:tags] (wild String)
   [:type] (wild String)
   [:users_in_photo] (wild String)
   [:caption :created_time] (wild String)
   [:caption :id] (wild String)
   [:caption :text] (wild String)
   [:comments :count] (wild String)
   [:comments :data] (wild String)
   [:likes :count] (wild String)
   [:likes :data] (wild String)
   [:user :bio] (wild String)
   [:user :full_name] (wild String)
   [:user :id] (wild String)
   [:user :profile_picture] (wild String)
   [:user :username] (wild String)
   [:user :website] (wild String)
   [:caption :from :full_name] (wild String)
   [:caption :from :id] (wild String)
   [:caption :from :profile_picture] (wild String)
   [:caption :from :username] (wild String)
   [:images :low_resolution :height] (wild Number)
   [:images :low_resolution :url] (wild String)
   [:images :low_resolution :width] (wild Number)
   [:images :standard_resolution :height] (wild Number)
   [:images :standard_resolution :url] (wild String)
   [:images :standard_resolution :width] (wild Number)
   [:images :thumbnail :height] (wild Number)
   [:images :thumbnail :url] (wild String)
   [:images :thumbnail :width] (wild Number)])

(def-example-factory instagram instagram-schema []
  {:attribution nil
   :link "http://instagram.com/p/f3SEeEAMnd/"
   :created_time "1382646982"
   :filter "Normal"
   :images {:low_resolution
            {:url "http://distilleryimage9.s3.amazonaws.com/f1d78c1a3ceb11e39bfb22000ab6846a_6.jpg"
             :width 306
             :height 306}
            :thumbnail
            {:url "http://distilleryimage9.s3.amazonaws.com/f1d78c1a3ceb11e39bfb22000ab6846a_5.jpg"
             :width 150
             :height 150}
            :standard_resolution
            {:url "http://distilleryimage9.s3.amazonaws.com/f1d78c1a3ceb11e39bfb22000ab6846a_8.jpg"
             :width 640
             :height 640}}
   :location nil
   :caption {:created_time "1382647332"
             :text "#nokia #1520 #cnet\n#windows"
             :from
             {:username (random-str 15)
              :profile_picture "http://images.ak.instagram.com/profiles/profile_428135049_75sq_1378953230.jpg"
              :id "428135049"
              :full_name (random-str 15)}
             :id "574009892348676727"}
   ;; TODO, Thu Oct 24 2013, Francis Wolke
   ;; This should be a sub schema
   :likes {:count 2
           :data
           [{:username (random-str 15)
             :profile_picture "http://images.ak.instagram.com/profiles/profile_46819872_75sq_1380936134.jpg"
             :id "46819872"
             :full_name (random-str 15)}
            {:username "victoriahassecret123"
             :profile_picture
             "http://images.ak.instagram.com/profiles/profile_605244607_75sq_1381806201.jpg"
             :id "605244607"
             :full_name "Victoria\nHuynh"}]}
   :users_in_photo []
   :type "image"
   :user {:username (random-str 15)
          :website ""
          :profile_picture
          "http://images.ak.instagram.com/profiles/profile_428135049_75sq_1378953230.jpg"
          :full_name (random-str 15)
          :bio ""
          :id "428135049"}
   :comments {:count 0 :data []}
   :id "574006949507549661_428135049"
   :tags ["cnet" "windows" "nokia" "1520"]})

; Utils
;*******************************************************************************

;;; TODO, Thu Oct 24 2013, Francis Wolke
;;; Nothing here is being sorted

(defn n-sorted-tweets
  "n tweets, sorted by timestamp, oldest first"
  [n]
  (sort-datums-by-timestamp (take n (iterate (fn [_] (tweet)) (tweet)))))

(defn n-sorted-instagrams
  "n instagrams, sorted by timestamp, oldest first"
  [n]
  )

(defn n-sorted-datums
  "n datums, sorted by timestamp, oldest first"
  [n]
  )
