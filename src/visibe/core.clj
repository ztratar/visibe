(ns ^{:doc "Starting point for the application."}
  visibe.core
  (:use hiccup.core)
  (:require [clj-http.lite.client :as client]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [cheshire.core :refer [decode]]
            [monger.core :as mg]
            [org.httpkit.server :as hk]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [monger.query :as q]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.format :refer [show-formatters]]
            [monger.operators :refer :all]
            [monger.collection :as c])
  (:import org.bson.types.ObjectId))

(def state (atom {:channels #{}
                  :last-req nil
                  :server nil
                  :req nil
                  :nrepl-server nil}))

; Connection
;*******************************************************************************

(def conn-info
  {:username "francis"
   :password "Fary15243%"
   :host "ds047438.mongolab.com"
   :database "visibe-dev"
   :port 47438})

(defn conn-uri
  "'%' is an escape character."
  [{username :username password :password host :host port :port database :database}]
  (let [password (if (some #{\%} password)
                   (clojure.string/replace password "%" "%25") password)]
    (str "mongodb://" username ":" password "@" host ":" port "/" database)))

(def mongo-uri (conn-uri conn-info))

(defn google-trends []
  ;; NOTE, Mon Sep 30 2013, Francis Wolke
  ;; For the time being we don't need phantom.js as they only update this when
  ;; google does, and don't appear to care that we're hitting their API.
  (decode (:body (client/get "http://hawttrends.appspot.com/api/terms/"))))

(def google-mapping
  ;; NOTE, Mon Sep 30 2013, Francis Wolke
  ;; I have no idea why they're ranked like this (and have missing keys). The
  ;; second version of the should be able to figure out countries itself.
  {"1" :united-states
   "3" :india
   "4" :japan
   "5" :singapore
   "6" :israel
   "8" :australia
   "9" :united-kingdom
   "10" :hong-kong
   "12" :taiwan
   "13" :canada
   "14" :russia
   "15" :germany})

(defn keys->countries
  [m]
  (loop [ks (keys m)
         acc {}]
    (if (empty? ks)
      acc
      (let [k (first ks)]
        (recur (rest ks)
               (assoc acc (google-mapping k) (m k)))))))

(defn persist-trends [m]
    (c/insert "google-trends" (keys->countries m)))

; Server
;*******************************************************************************

(defn ws-handler
  [request]
  (letfn [(close-chan [status] (swap! state update-in [:channels] remove channel))]
    (hk/with-channel request channel
      (swap! state update-in [:chan] conj channel)
      (swap! state assoc :req request)
      (hk/on-close channel close-chan)
      (hk/on-receive channel
                     (fn [data] (hk/send! channel (str "ECHO:" data) false))))))

(defroutes app
  (GET "/" [] (html [:h1 "Server running"]))
  (GET "/ws" [] ws-handler)
  (route/resources "/"))

(defn run-server
  "We only run the main loop when passed two args. A single arg is used for
development."
  ([port]
     (mg/connect-via-uri! mongo-uri)
     (swap! state assoc-in [:server]
            (hk/run-server #'app {:port (Integer. port)})))
  ([port nrepl-port]
     (mg/connect-via-uri! mongo-uri)
     ;; XXX, Mon Sep 30 2013, Francis Wolke
     ;; Side effecting code!
     (future
       (loop [trends (google-trends)]
         ;; 5 min
         (Thread/sleep 300000)
         (recur (let [data (google-trends)]
                  (when-not (= trends data)
                    (persist-trends data)
                    data))))))
  (swap! state assoc-in [:nrepl-server]
         (start-server :port (Integer. nrepl-port)))
  (swap! state assoc-in [:server]
         (hk/run-server #'app {:port (Integer. port)}))))

(defn main- [& [port nrepl-port]]
  (if (and port nrepl-port) (run-server port nrepl-port)
      (println "lein run <port> <nrepl-port>")))
