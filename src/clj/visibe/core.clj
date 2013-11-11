(ns ^{:doc "Starting point."}
  visibe.core
  (:use user
        hiccup.core
        hiccup.page
        ring.middleware.refresh)
  (:require [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [visibe.feeds.storage :as storage]
            [visibe.feeds.twitter :refer [new-bearer-token!]]
            [visibe.feeds.instagram :as instagram]
            [visibe.feeds :as feeds]
            [visibe.api :refer [websocket-handler]]
            [cheshire.core :refer [decode]]
            [visibe.state :refer [state gis assoc-in-state! read-config!]]
            [monger.core :as mg]
            [org.httpkit.server :as hk]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]))

(defn index []
  (html5 [:head
          [:title "Visibe - Watch situations and reactions unfold as they happen"]
          [:meta {:charset "UTF-8"}]
          [:link {:rel "stylesheet/less" :type "text/css" :href "css/styles.less"}]
          [:link {:rel "stylesheet" :type "text/css" :href "font-awesome/css/font-awesome.min.css"}]]
          [:body [:script {:type "text/javascript"}
                  "less = {
                   env: \"development\", // or \"production\"
                   async: false,       // load imports async
                   fileAsync: false,   // load imports async when in a page under
                                       // a file protocol
                   poll: 1000,         // when in watch mode, time in ms between polls
                   functions: {},      // user functions, keyed by name
                   dumpLineNumbers: \"comments\", // or \"mediaQuery\" or \"all\"
                   relativeUrls: false,// whether to adjust url's to be relative
                                       // if false, url's are already relative to the
                                       // entry less file
                   rootpath: \"css/\"// a path to add on to the start of every url
                                       //resource
           };"]
                   (include-js "js/libs/d3.v3.min.js"
                               ;; "https://apis.google.com/js/plusone.js"
                               "js/visibe_dbg.js" "js/libs/video-js/video.js"
                               "js/libs/less.js"
                               "js/libs/moment-with-langs.js")
                   [:div#content]]
         ))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/ws" [] websocket-handler)
  ;; Defaults to PROJECT_ROOT/resouces/public 
  (route/resources "/"))             

(def app (handler/site app-routes))

(defn rally-the-troops!
  [mode]
  (read-config!)
  (mg/connect-via-uri! (storage/conn-uri (:mongo @state)))
  (new-bearer-token!)
  (start-server :port (Integer. (get-in @state [:app :nrepl-port])))
  (assoc-in-state! [:app :server] (hk/run-server #'app {:port (Integer. (get-in @state [:app :port]))}))
  (instagram/generate-oauth-creds!)
  (case mode
    :dev (feeds/scrape-trends!)
    :production (feeds/scrape-and-persist-trends!)))

(defn main-
  ([] (println "Please specify one of #{help dev production}"))
  ([mode] (case mode
            "production" (rally-the-troops! :production)
            "dev" (rally-the-troops! :dev)
            "help" (println "`lein run dev` will start the server without storing data in the database. `lein run production` will. In either case, HTTP/WebSocket and nREPL servers will start with the ports specified in PROJECT_ROOT/config.cljd")
            (println "Please specify one of #{help dev production}"))))
