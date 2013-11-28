(ns ^{:doc "Clean data from social media sites

            There exists both `clean-X' and `X->essentials' because the data
            we wish to store does not map directly to what we want to send to
            the client. `clean-X' should be used only when dealing with raw data
            from a social media source, and `X->essentials' should be used when
            sending data to a client"}
  visibe.feeds.sanitation
  (:use user)
  (:require [visibe.homeless :refer [rfc822-str->long]]
            [clojure.set :refer [rename-keys]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Instagram

(defn clean-instagram [trend instagram]
  (-> instagram
      (assoc :datum-type (if (= "image" (:type instagram))
                           "instagram-photo" "instagram-video")
             :trend trend)
      (rename-keys {:created_time :created-at})
      ;; Java time is in millis, UNIX time is in seconds
      (update-in [:created-at] #(* 1000 (Integer/parseInt %)))))

(defn instagram->essentials [m]
  (let [a (partial get-in m)
        i (= "instagram-photo" (:datum-type m))]
    (-> m
        (select-keys [:tags :id :created-at :link :trend :datum-type :_id])
        (merge {:full-name (a [:user :full_name])
                :profile-picture (a [:user :profile_picture])
                :username (a [:user :username])}
               (if i
                 ;; TODO, Sat Nov 23 2013, Francis Wolke
                 ;; These could be combined into ':instagram' or ':media'
                 {:photo (a [:images :low_resolution :url])}
                 {:video (a [:videos :standard_resolution :url])})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Twitter

(defn- twitter-time->long
  [s]
  (let [[weekday month day time ? year] (clojure.string/split s #" ")]
    (rfc822-str->long (clojure.string/join " " [(str weekday ",") day month year time ?]))))

(defn clean-tweet  
  "XXX, Sun Nov 24 2013, Francis Wolke

   Tweets don't obey a sane schema. If ':retweeted_status' is a member of a
   tweet, then, for whatever reason, you'll find that the CORRECT ':created-at'
   is actually in ':retweeted_status :created_at'. There is no documentation
   that explains this, and googling pulls up nothing.

   Aside from that, this puts tweets into the format that we'd prefer to handle"
  [trend tweet]
  (letfn [(->created-at [m]
            (update-in (rename-keys m {:created_at :created-at})
                       [:created-at] twitter-time->long))

          (clean-img-tweet [tweet]
            (update-in tweet [:retweeted_status] ->created-at))

          ;; TODO, XXX, FIXME Wed Nov 27 2013, Francis Wolke
          ;; Currently does not support video
          (clean-video-tweet [tweet])]

    (let [datum-type (cond (-> tweet :retweeted_status :entities :media) "tweet-photo"
                           ;; (...) video
                           :else "tweet")
          tweet (-> tweet
                    (assoc :trend trend)
                    (assoc :datum-type datum-type)
                    ->created-at)]

      (case datum-type
        ;; "video-tweet" (throw (Exeception. "clean tweet attempted to clean a video tweet, which is not supported"))
        "tweet-photo" (clean-img-tweet tweet)
        "tweet" tweet))))

(defn vanilla-tweet->essentials [tweet]
  (merge (select-keys tweet [:text :created-at :trend :datum-type :id_str :_id])
         (select-keys (:user tweet) [:name :screen_name :profile_image_url_https])
         {:link-urls (mapv #(select-keys % [:url :indices]) (-> tweet :entities :urls ))}))

(defn tweet-photo->essentials [tweet]
  (merge (vanilla-tweet->essentials tweet)
         (select-keys (:retweeted_status tweet) [:created-at])
         ;; Photo
         {:photo-url (:media_url (first (-> tweet :retweeted_status :entities :media)))}))

(defn video-tweet->essentials [tweet])

(defn tweet->essentials
  [tweet]
  ((case (:datum-type tweet)
     "tweet-photo" tweet-photo->essentials
     "video-tweet" video-tweet->essentials
     "tweet"       vanilla-tweet->essentials) tweet))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Common

(defn datum->essentials [datum]
  (-> (if (= "tweet" (:datum-type datum))
        (tweet->essentials datum)
        (instagram->essentials datum))
      (update-in [:_id] str)))
