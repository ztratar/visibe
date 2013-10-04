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


;;; Connection

(defn conn-uri
  "'%' is an escape character."
  [{username :username password :password host :host port :port database :database}]
  (let [password (if (some #{\%} password)
                   (clojure.string/replace password "%" "%25") password)]
    (str "mongodb://" username ":" password "@" host ":" port "/" database)))

;;; Persistence

(defn persist-trends [m]
  (c/insert "trends" m))

;;; Query

;; (def trends-q (partial (c/find-maps "trends")))
