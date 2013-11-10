(ns ^{:doc "Data Storage and retrieval."}
  visibe.feeds.storage
  (:use visibe.homeless)
  (:require [monger.core :as mg]
            [monger.query :as q]
            [clj-time.local :refer [local-now format-local-time]]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [monger.operators :refer :all]
            [monger.collection :as c])
  (:import org.bson.types.ObjectId))

(defn conn-uri
  "'%' is an escape character."
  [{username :username password :password host :host port :port database :database}]
  (let [password (if (some #{\%} password)
                   (clojure.string/replace password "%" "%25")
                   password)]
    (str "mongodb://" username ":" password "@" host ":" port "/" database)))

(defn persist-google-trends-and-photos
  "Does what you would think, and saves the time that the transaction occured as
':created-at' so that we can reference this data at a later date"
  [trends-and-photos-hashmap]
  (->> trends-and-photos-hashmap
       (merge {:created-at (to-long (format-local-time (local-now) :date-time))})
       (c/insert "google-trends")))

(defn create-trend
  "Create trend if it does not exist"
  [trend]
  (when-not (c/find-one-as-map "trends" {:trend trend})
    (c/insert "trends"
              {:trend trend
               :datums []
               ;; XXX, Tue Oct 08 2013, Francis Wolke
                 
               ;; I don't want to do this, but mongodb dosn't have a way of
               ;; timestamping transactions. Look at other datastores?
               ;; Also, I do belive that this is the wrong time
               ;; format. Though, for the next day or two it won't matter.
               :created-at (to-long (format-local-time (local-now) :date-time))})))

(defn append-datums
  "Adds new datums to the tail of the trend's datum seq. Datums must be sorted
by timestamp prior to appending.

XXX, Tue Oct 08 2013, Francis Wolke

This function ASSUMES that you will ONLY be appending datums that are
newer than those already in ':datums'"
  [trend new-datums]
  ;; TODO, Tue Oct 08 2013, Francis Wolke
  ;; This is horrible, I find it really hard to belive that there isn't an
  ;; append function (native to mongodb). Revisit.
  (let [{datums :datums :as m} (c/find-one-as-map "trends" {:trend trend})]
    (c/update "trends" m (assoc m :datums (into datums new-datums)))))

(defn previous-50-datums 
  "Retuns 50 datums chronologically previous to the supplied datum"
  [trend datum]
  (let [{datums :datums} (c/find-one-as-map "trends" {:trend trend})
        older (take-while (partial not= datum) datums)]
    (if (> (count older) 50)
      (drop (- (count older) 50) older)
      older)))

(defn after-datum
  "Returns any datums that come chronologically after the supplied datom"
  [trend supplied-datum]
  (if (nil? supplied-datum)
    (:datums (c/find-one-as-map "trends" {:trend trend}))
    (let [{datums :datums} (c/find-one-as-map "trends" {:trend trend})]
      (rest (drop-while (partial not= supplied-datum) datums)))))

(defn most-recent-google-trend
  []
  (dissoc (c/find-one-as-map "google-trends") :_id))

(defn add-new-trends
  "Trends and their associated images"
  [trend-and-images-hashmap]
  (c/insert "google-trends" trend-and-images-hashmap))

(defn intial-trend-datums
  "Accepts a seq of trends, returns a seq of the most 20 recent datums of each type. "
  [trends]
  (reduce into (map (fn [trend] (map #(assoc % :trend trend)
                                     (:datums (c/find-one-as-map "trends" {:trend trend})))) trends)))
