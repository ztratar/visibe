(ns ^{:doc "Clean up the data from social media sites

            There exists both `clean-X' and `X->essentials' because the data
            we wish to store does not map directly to what we want to send to
            the client. `clean-X' should be used only when dealing with raw data
            from a social media source, and `X->essentials' should be used when
            sending data to a client."}
  visibe.feeds.sanitation
  (:require [visibe.homeless :refer [rfc822-str->long]]
            [clojure.set :refer [rename-keys]]))

(defn clean-instagram [trend instagram]
  (-> instagram
      (assoc :datum-type (if (= "image" (:type instagram))
                           :instagram-photo :instagram-video)
             :trend trend)
      (rename-keys {:created_time :created-at})
      ;; Java time is in millis, UNIX time is in seconds
      (update-in [:created-at] #(* 1000 (Integer/parseInt %)))))

(defn instagram->essentials [m]
  (let [a (partial get-in m)
        i (= :instagram-photo (:datum-type m))]
    (-> m
        (select-keys [:tags :id :created-at :link :trend])
        (merge {:full-name (a [:user :full_name])
                :profile-picture (a [:user :profile_picture])
                :username (a [:user :username])}
               (if i
                 ;; TODO, Sat Nov 23 2013, Francis Wolke
                 ;; These could be combined into ':instagram' or ':media'
                 {:photo (a [:images :standard_resolution])}
                 {:video (a [:videos :standard_resolution])})))))

(defn- twitter-time->long
  [s]
  (let [[weekday month day time ? year] (clojure.string/split s #" ")]
    (rfc822-str->long (clojure.string/join " " [(str weekday ",") day month year time ?]))))

(defn clean-tweet
  "Takes a raw tweet, turns it into the format we'd prefer to handle"
  [trend tweet]
  (-> tweet
      (assoc :trend trend)
      (assoc :datum-type :tweet)
      (rename-keys {:created_at :created-at})
      (update-in [:created-at] twitter-time->long)))

(defn tweet->essentials
  ;; XXX, NOTE Fri Oct 04 2013, Francis Wolke
  ;; `:text` path does not always have full urls.
  [tweet]
  (merge (select-keys tweet [:text :created-at :trend :datum-type])
         (select-keys (:user tweet) [:name :screen_name :profile_image_url_https])))
