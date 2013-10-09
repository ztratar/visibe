(ns ^{:doc "Data Storage and retrieval."}
  visibe.feeds.storage
  (:require [monger.core :as mg]
            [monger.query :as q]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :refer [show-formatters]]
            [monger.operators :refer :all]
            [monger.collection :as c])
  (:import org.bson.types.ObjectId))

;;; Connection

(defn conn-uri
  "'%' is an escape character."
  [{username :username password :password host :host port :port database :database}]
  (let [password (if (some #{\%} password)
                   (clojure.string/replace password "%" "%25") password)]
    (str "mongodb://" username ":" password "@" host ":" port "/" database)))

;;; Persistence

(defn create-trend [trend]
  (c/insert "test" {:trend trend
                    ;; :created-at (now)
                    :datums []}))

(defn append-datums
  "Adds new datums to the tail of the trend's datum seq. Datums must be sorted
by timestamp prior to appending"
  [trend new-datums]
  ;; TODO, Tue Oct 08 2013, Francis Wolke
  ;; This is horrible, I find it really hard to belive that there isn't an
  ;; append function. Revisit.
  (let [{datums :datums :as m} (c/find-one-as-map "test" {:trend trend})]
    (c/update "test" m (assoc m :datums (into datums new-datums)))))

(defn previous-50-datums
  "Retuns 50 chronologically previous to the supplied datum"
  [trend datum]
  (let [{datums :datums} (c/find-one-as-map "test" {:trend trend})]
    (take 50 (drop-while #(not= datum) datums))))
