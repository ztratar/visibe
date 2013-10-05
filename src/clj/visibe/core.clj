(ns ^{:doc "Starting point."}
  visibe.core
  (:use hiccup.core
        hiccup.page
        ring.middleware.refresh)
  (:require [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [visibe.storage :refer [conn-uri]]
            [visibe.rpc :refer [rpc-call]]
            [visibe.homeless :refer [test-handle]]
            [visibe.feeds.google-trends :refer [srape-google-trends]]
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
      (swap! state update-in [:app :channels] conj channel)
      ;; (swap! state assoc :req request) I don't think we need anything from
      ;; the request.
      (hk/on-close channel (partial close-chan channel))
      ;; NOTE, Thu Oct 03 2013, Francis Wolke
      ;; If we run this in a future we won't be tied so a single thread of
      ;; execution, but the messages won't come back in order.
      (hk/on-receive channel
                     ;; (partial rpc-handler channel)
                     ;; (fn [data] (hk/send! channel (rpc-call data)
                     ;;                      false))
                     (fn [data] (test-handle channel data))
                     ))))

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
     (srape-google-trends)
     (map (fn [[p f]] (update-state! p f))
          [
           [[:app :nrepl-server] (start-server :port (Integer. nrepl-port))]
           [[:app :server] (hk/run-server #'app {:port (Integer. port)})]
           ])))

(defn read-config [config-path]
  ;; TODO, Wed Oct 02 2013, Francis Wolke
  ;; Use clj-schema to verify config file is correct.
  (defonce state (atom (read-string (slurp config-path))))
  (doseq [[k v] [[:channels #{}]
                 [:last-req nil]
                 [:server nil]
                 [:req nil]
                 [:nrepl-server nil]]]
    (swap! state assoc-in [:app k] v)))

(defn main- 
  ([]
     (read-config "./config.cljd")
     (rally-the-troops (:port (:app @state)) (:nrepl-port (:app @state))))
  ([config-path]
     (read-config config-path)
     (rally-the-troops (:port (:app @state)) (:nrepl-port (:app @state)))))
