(ns ^{:doc "Data Storage and retrieval."}
  visibe.storage
  (:require [monger.core :as mg]
            [monger.query :as q]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :refer [show-formatters]]
            [monger.operators :refer :all]
            [monger.collection :as c])
  (:import org.bson.types.ObjectId))

;;; Development stuff.
;;; (c/remove "foo")

;;; Connection

(defn conn-uri
  "'%' is an escape character."
  [{username :username password :password host :host port :port database :database}]
  (let [password (if (some #{\%} password)
                   (clojure.string/replace password "%" "%25") password)]
    (str "mongodb://" username ":" password "@" host ":" port "/" database)))

;;; Persistence

(defn persist-trends [m]
  (c/insert "test-trends" m))

(defn persist-tweets
  ;; XXX, Sat Oct 05 2013, Francis Wolke
  
  ;; All inserts are currently happening here, and the google trend data is
  ;; being held in memory.
  [trend tweets]
  (if (empty? (c/find-maps "test-trends" {:trend trend}))
    (c/insert "test-trends" {:trend trend :datums tweets})
    (c/update "test-trends" {:trend trend} {:datums tweets} :upsert true)))
