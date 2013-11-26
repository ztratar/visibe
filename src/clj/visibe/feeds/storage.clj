(ns ^{:doc "Data Storage and retrieval."}
  visibe.feeds.storage
  (:use visibe.homeless user)
  (:require [monger.core :as mg]
            [monger.query :as q]
            [visibe.feeds.sanitation :refer [datum->essentials]]
            [clojure.set :refer [rename-keys]]
            [clj-time.local :refer [local-now format-local-time]]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [monger.query :as q]
            [monger.operators :refer :all]
            [monger.collection :as c])
  (:import org.bson.types.ObjectId
           com.mongodb.WriteConcern))

(defn remove-trend-collections []
  (doseq [i (remove #{"fs.chunks" "fs.files" "google-trends" "system.indexes" "system.users"}
                    (.getCollectionNames mg/*mongodb-database*))]
    (c/drop i)))

(defn conn-uri
  "'%' is an escape character."
  [{username :username password :password host :host port :port database :database}]
  (let [password (if (some #{\%} password)
                   (clojure.string/replace password "%" "%25")
                   password)]
    (str "mongodb://" username ":" password "@" host ":" port "/" database)))

(defn persist-google-trends-and-photos
  "Saves the time that the transaction occured as
   ':created-at' so that we can reference this data at a later date"
  [trends-and-photos-hashmap]
  (->> trends-and-photos-hashmap
       (merge {:created-at (to-long (format-local-time (local-now) :date-time))})
       (c/insert "google-trends")))

(defn append-datums
  "Adds datums to a trend, creates the trend if it didn't already exist"
  [trend datums]
  (c/insert-batch trend datums)
  (c/ensure-index trend (array-map :created-at -1)
                  {:unique true :dropDupes true :background true}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Queries

(defn datums-since
  "Returns datums younger than TIME for given trend"
  [trend time]
  (->> (q/with-collection trend
         (q/find {:created-at (array-map $gt time)})
         (q/sort (array-map :created-at -1)))
       (map #(datum->essentials (dissoc % :_id)))))

(defn previous-15
  "Returns 15 datums older than the supplied datum for a given trend"
  ;; FIXME, Wed Nov 13 2013, Francis Wolke
  ;; We will discard valid data here, if two datums happen to have the same timestamp
  [{trend :trend created-at :created-at}]
  (->> (q/with-collection trend
         (q/find {:created-at (array-map $lt created-at)})
         (q/sort (array-map :created-at -1))
         (q/limit 15))
       (map #(datum->essentials (dissoc % :_id)))))

(defn seed-datums
  "20 most recent datums on a trend"
  [trend]
  (->> (q/with-collection trend
         (q/sort (array-map :created-at -1))
         (q/limit 20))
       (map #(datum->essentials (dissoc % :_id)))))

(defn most-recent-datum [trend]
  (first (->> (q/with-collection trend
                (q/sort (array-map :created-at -1))
                (q/limit 1))
              (map #(datum->essentials (dissoc % :_id))))))
