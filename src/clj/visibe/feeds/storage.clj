(ns ^{:doc "Data Storage and retrieval."}
  visibe.feeds.storage
  (:use visibe.homeless user)
  (:require [monger.core :as mg]
            [monger.query :as q]
            [clojure.set :refer [rename-keys]]
            [clj-time.local :refer [local-now format-local-time]]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :as f]
            [monger.query :as q]
            [monger.operators :refer :all]
            [monger.collection :as c])
  (:import org.bson.types.ObjectId))

(defn instagram-photo->essentials [m]
  (let [a (partial get-in m)]
    (-> m
        (select-keys [:tags :id :created-at :link :trend :datum-type])
        (merge {:full-name (a [:user :full_name])
                :profile-picture (a [:user :profile_picture])
                :username (a [:user :username])
                :photo (a [:images :standard_resolution])}))))

(defn instagram-video->essentials [m]
  (let [a (partial get-in m)]
    (-> m
        (select-keys [:tags :id :created-at :link :trend :datum-type])
        (merge {:full-name (a [:user :full_name])
                :profile-picture (a [:user :profile_picture])
                :username (a [:user :username])
                :video (a [:videos :standard_resolution])}))))

(defn tweet->essentials
  ;; FIXME, NOTE Fri Oct 04 2013, Francis Wolke
  ;; `:text` path may not always have full urls.
  [tweet]
  (-> (merge (select-keys tweet [:text :created-at :trend :datum-type])
             (select-keys (:user tweet) [:name :screen_name :profile_image_url_https]))))

(defn clean-datum [datum]
  (case (keyword (:datum-type datum))
    :instagram-photo (instagram-photo->essentials datum)
    :instagram-video (instagram-video->essentials datum)
    :tweet           (tweet->essentials datum)
    datum))

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
  ;; TODO, Wed Nov 13 2013, Francis Wolke
  ;; Handle batch insert errors
  ;; http://www.mongodb.org/display/DOCS/Inserting#Inserting-Bulkinserts
  ;; We should be ensuring datum uniquness here. But this will cause an
  ;; exception to be thrown if there are duplicates.
  [trend datums]
  (and (c/insert-batch trend datums)
       ;; Use `and' to ensure that the write has returned before indexing
       (c/ensure-index trend (array-map :created-at -1)
                       ;; {:unique true}
                       )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Datum Queries

(defn datums-since
  "Returns datums younger than TIME for given trend"
  [trend time]
  (->> (q/with-collection trend
         (q/find {:created-at (array-map $gt time)})
         (q/sort (array-map :created-at -1)))
       (map #(clean-datum (dissoc % :_id)))))

(defn previous-15
  "Returns 15 datums older than the supplied datum for a given trend"
  ;; FIXME, Wed Nov 13 2013, Francis Wolke
  ;; We will discard valid data here, is two datums happen to have the same timestamp
  [{trend :trend created-at :created-at}]
  (->> (q/with-collection trend
         (q/find {:created-at (array-map $lt created-at)})
         (q/sort (array-map :created-at -1))
         (q/limit 15))
       (map #(clean-datum (dissoc % :_id)))))

(defn seed-datums
  "Returns 15 most recent datums on a trend"
  [trend]
  (->> (q/with-collection trend
         (q/sort (array-map :created-at -1))
         (q/limit 15))
       (map #(clean-datum (dissoc % :_id)))))

(defn most-recent-datum [trend]
  (first (->> (q/with-collection trend
                (q/sort (array-map :created-at -1))
                (q/limit 1))
              (map #(clean-datum (dissoc % :_id))))))
