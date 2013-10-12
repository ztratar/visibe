(ns ^{:doc "Starting point."}
  visibe.core
  (:use user
        ring.middleware.refresh)
  (:require [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [visibe.feeds.storage :as storage]
            [visibe.feeds.twitter :refer [new-bearer-token!]]
            [visibe.feeds :as feeds]
            [visibe.api :refer [api-routes websocket-handler]]
            [visibe.template :refer [index]]
            [cheshire.core :refer [decode]]
            [visibe.state :refer [state update-state! read-config!]]
            [monger.core :as mg]
            [org.httpkit.server :as hk]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/ws" [] websocket-handler)
  api-routes
  ;; Defaults to PROJECT_ROOT/resouces/public 
  (route/resources "/"))             

(def app (handler/site app-routes))

(defn rally-the-troops!
  [mode]
  (read-config!)
  (mg/connect-via-uri! (storage/conn-uri (:mongo @state)))
  (new-bearer-token!)
  (start-server :port (Integer. (get-in @state [:app :nrepl-port])))
  (update-state! [:app :server] (hk/run-server #'app {:port (Integer. (get-in @state [:app :port]))}))
  (case mode
    :dev (feeds/dev!)
    :production (feeds/production!)))

(defn main-
  ([] (println "Please specify one of #{help dev production}"))
  ([mode] (case mode
            "production" (rally-the-troops! :production)
            "dev" (rally-the-troops! :dev)
            "help" (println "`lein run dev` will start the server without storing data in the database. `lein run production` will. In either case, HTTP/WebSocket and nREPL servers will start with the ports specified in PROJECT_ROOT/config.cljd")
            (println "Please specify one of #{help dev production}"))))
