(ns ^{:doc "Starting point."}
  visibe.core
  (:use hiccup.core
        hiccup.page
        ring.middleware.refresh)
  (:require [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [visibe.feeds.storage :as storage]
            [visibe.feeds :as feeds]
            [visibe.api :as api]
            [visibe.homeless :refer [test-handle]]
            [cheshire.core :refer [decode]]
            [visibe.state :refer [state read-config!]]
            [monger.core :as mg]
            [org.httpkit.server :as hk]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]))

(defn index []
  (html5 [:head
          [:title "Visibe - Watch situations and reactions unfold as they happen"]
          [:meta {:charset "UTF-8"}]
          
          (include-css "css/fonts.css" "css/bootstrap.css" "css/style.css")]

         [:body [:div {:class "page-wrapper"}
                 [:a {:href "/" :class "home"} "Home"]
                 [:a {:href "/" } [:h1 {:id "logo"} "Visible"]]
                 [:p {:id "main-byline"} "Watch situations and reactions unfold as they happen"]
                 [:div {:class "page-container"}]]

          (include-js "js/libs/jquery.js" "js/libs/underscore.js"
                      "js/libs/backbone.js" "js/libs/bootstrap.js"
                      "js/app.js" "js/visibe_dbg.js" "js/d3.v3.min.js")
          
          [:script {:type "text/template" :id "TopicCardView-template"}
           [:a {:href "/topic/4"}
            [:h2 "Topic Name"]
            [:span "byline"]]]
          
          [:script {:type "text/template" :id "TopicView-template"}
           [:img {:class "topic-img" :src ""}]
           [:h2 "Topic Name"]
           [:span "timeline-container"]]]))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/ws" [] api/websocket-handler)
  
  ;; (api/routes)
  ;; (POST "/ping" {body :body} (let [req-data (read-string (slurp body))] req-data))
  ;; (POST "/api/previous-datums" {body :body}  (let [] (api/previous-datums id)))
  ;; (POST "/api/current-trends" {body :body} (str (api/current-trends)))
  
  ;; Defaults to PROJECT_ROOT/resouces/public 
  (route/resources "/"))             

(def app (handler/site app-routes))

(defn rally-the-troops!
  [mode]
  (mg/connect-via-uri! (storage/conn-uri (:mongo @state)))
  (map (fn [[p f]] (update-state! p f))
       [[[:app :nrepl-server] (start-server :port (Integer. (get-in @state [:app :nrepl-port])))]
        [[:app :server] (hk/run-server #'app {:port (Integer. (get-in @state [:app :port]))})]])
  (case mode
    :dev (feeds/dev!)
    :production (feeds/production!)))

(defn main-
  ([] (println "Please specify one of #{help dev production}"))
  ([mode] (case mode
            "production" (do (read-config!) (rally-the-troops! :production))
            "dev" (do (read-config!) (rally-the-troops! :dev))
            "help" (println "`lein run dev` will start the server without storing data in the database. `lein run production` will. In either case, HTTP/WebSocket and nREPL servers will start with the ports specified in PROJECT_ROOT/config.cljd")
            (println "Please specify one of #{help dev production}"))))
