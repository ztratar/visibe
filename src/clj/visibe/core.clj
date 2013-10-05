(ns ^{:doc "Starting point."}
  visibe.core
  (:use hiccup.core
        hiccup.page
        ring.middleware.refresh)
  (:require [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [visibe.storage :refer [conn-uri]]
            [visibe.homeless :refer [test-handle]]
            [cheshire.core :refer [decode]]
            [monger.core :as mg]
            [org.httpkit.server :as hk]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]))

(declare state)

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
                      "js/app.js" "js/visibe_dbg.js"
                      ;; "js/d3.v3.min.js"
                      )
          
          [:script {:type "text/template" :id "TopicCardView-template"}
           [:a {:href "/topic/4"}
            [:h2 "Topic Name"]
            [:span "byline"]]]
          
          [:script {:type "text/template" :id "TopicView-template"}
           [:img {:class "topic-img" :src ""}]
           [:h2 "Topic Name"]
           [:span "timeline-container"]]]))

;;; NOTE, Fri Oct 04 2013, Francis Wolke
;;; Broken connections are to be buildind up in `state'.
(defn ws-handler
  [request]
  (letfn [(close-chan [channel status]
            (swap! state update-in [:app :channels] remove channel))]
    (hk/with-channel request channel
      ;; Remove me.
      (swap! state update-in [:app :channels] conj channel)
      ;; I don't think any information from the request is currently needed.
      ;; (swap! state assoc :req request) 
      (hk/on-close channel (partial close-chan channel))
      (hk/on-receive channel
                     (fn [data] (test-handle channel data))))))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/ws" [] ws-handler)
  (route/resources "/"))             ; Defaults to PROJECT_ROOT/resouces/public 

(def app (handler/site app-routes))

(defn update-state! [path form]
  (swap! state assoc-in path form))

(defn rally-the-troops
  "Run the main loop when passed two args. A single arg is used for development."
  ([port]
     (mg/connect-via-uri! (conn-uri (:mongo @state)))
     (update-state! [:app :server] (hk/run-server #'app {:port (Integer. port)})))
  
  ([port nrepl-port]
     (mg/connect-via-uri! (conn-uri (:mongo @state)))
     ;; (srape-google-trends)
     (map (fn [[p f]] (update-state! p f))
          [[[:app :nrepl-server] (start-server :port (Integer. nrepl-port))]
           [[:app :server] (hk/run-server #'app {:port (Integer. port)})]])))

(defn read-config [config-path]
  ;; TODO, Wed Oct 02 2013, Francis Wolke
  ;; Use clj-schema to verify config file is correct.
  ;; For interactive development, ensure that we only `def' a new atom if one does
  ;; not already exist. Add bang!
  (def state (atom (read-string (slurp config-path))))
  (doseq [[k v] [[:channels #{}]
                 [:last-req nil]
                 [:server nil]
                 [:req nil]
                 [:nrepl-server nil]]]
    (swap! state assoc-in [:app k] v)))

(defn dev-mode! []
  (read-config "./config.cljd")
  (rally-the-troops 4000 4001))

(defn main- 
  ;; ([]
  ;;    (read-config "./config.cljd")
  ;;    (rally-the-troops (:port (:app @state)) (:nrepl-port (:app @state))))
  ([]
     (dev-mode!))
  ([config-path]
     (read-config config-path)
     (rally-the-troops (:port (:app @state)) (:nrepl-port (:app @state)))))
