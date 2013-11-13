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

; Other
;*******************************************************************************

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

(defn sort-datums-by-timestamp
  "Returns datums with the newest first"
  [datums]
  (sort-by :created-at (map #(update-in % [:created-at] (fn [time] (if (number? time) time (Integer/parseInt time)))) datums)))

(defn after-datum
  ;; TODO, Wed Nov 13 2013, Francis Wolke
  ;; Convert all dates to numbers.
  ;; http://clojuremongodb.info/articles/querying.html
  "Returns any datums that come chronologically after the supplied datom"
  ([supplied-datum] (after-datum (:trend supplied-datum) supplied-datum))
  ([trend supplied-datum]
     (if (nil? supplied-datum)
       (:datums (c/find-one-as-map (str "trends") {:trend trend}))
       (let [{datums :datums} (c/find-one-as-map "trends" {:trend trend})]
         (rest (drop-while (partial not= supplied-datum) (sort-datums-by-timestamp datums)))))))

(defn most-recent-google-trend
  []
  (dissoc (c/find-one-as-map "google-trends") :_id))

(defn add-new-trends
  "Trends and their associated images"
  [trend-and-images-hashmap]
  (c/insert "google-trends" trend-and-images-hashmap))

(defn intial-datums
  "Accepts a seq of trends, returns a seq of the most 20 recent datums of each type. "
  [trends]
  (reduce into (map (fn [trend] (map #(assoc % :trend trend)
                                     (take 10 (:datums (c/find-one-as-map "trends" {:trend trend}))))) trends)))

;;; Issues

;;; We are not currently sorting are queries in mongo.
;;; Are we even getting any instagram data?

;; I want to test trends to see if their `datums' sub collection handles anything like

;; (def my-data (c/find-maps "trends" {:trend "Google Drive"}))

;; (count (:datums (first my-data)))

;; {:_id #<ObjectId 5283581eb0c6666547d7fe3c>, :datums {:1 {:name "test tweet", :datum-type "tweet", :tweet "stuff"}}, :trend "Food"}

;; (count (:datums my-data))

;; (c/insert "test" {:trend "Food" :datums {[]}})
;; (c/update "test" {:trend "Food" :datums [["foo" "bar" "baz"]]}
;;           :upsert true)

;; (c/find-maps "test" {:trend "Food"} [:datums])

;; (reduce into (map #(into #{} (map :datum-type (:datums %)))
;;                   (c/find-maps "trends")))
