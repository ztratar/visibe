(ns ^{:doc "Data Storage and retrieval."}
  visibe.feeds.storage
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
                   (clojure.string/replace password "%" "%25") password)]
    (str "mongodb://" username ":" password "@" host ":" port "/" database)))

(defn create-trend [trend]
  (c/insert "trends"
            {:trend trend
             :datums []

             ;; XXX, Tue Oct 08 2013, Francis Wolke
                    
             ;; I don't want to do this, but mongodb dosn't have a way of
             ;; timestamping transactions. Look at other datastores?
             ;; Also, I do belive that this is the wrong time
             ;; format. Though, for the next day or two it won't matter.
             :created-at (str (format-local-time (local-now) :date-time))}))

(defn append-datums
  "Adds new datums to the tail of the trend's datum seq. Datums must be sorted
by timestamp prior to appending.

XXX, Tue Oct 08 2013, Francis Wolke

This function ASSUMES that you will ONLY be appending datums that are
newer than those already in ':datums'"
  [trend new-datums]
  ;; TODO, Tue Oct 08 2013, Francis Wolke
  ;; This is horrible, I find it really hard to belive that there isn't an
  ;; append function. Revisit.
  (let [{datums :datums :as m} (c/find-one-as-map "trends" {:trend trend})]
    (c/update "trends" m (assoc m :datums (into datums new-datums)))))

(defn previous-50-datums 
  "Retuns 50 chronologically previous to the supplied datum"
  [trend datum]
  (let [{datums :datums} (c/find-one-as-map "trends" {:trend trend})]
    (take 50 (take-while (partial not= datum) datums))))

(defn after-datum
  ;; TODO, Tue Oct 08 2013, Francis Wolke
  ;; What about when we don't have the initial tweet?
  "Returns any tweets that come chronologically after the supplied datom"
  [trend supplied-datum]
  (let [{datums :datums} (c/find-one-as-map "trends" {:trend trend})]
    (rest (drop-while (partial not= supplied-datum) datums))))
