(ns eve.templates
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils])
  (:require-macros [dommy.macros :as m]))

(m/deftemplate home []
  ;; TODO, Mon Oct 14 2013, Francis Wolke
  ;; https://blog.mozilla.org/webdev/2009/02/20/cross-browser-inline-block/
  [:div#content
   [:div#title
    [:h1 "Visibe"]
    [:h2 "Watch social trends unfold in real-time"]]
   [:ul#trends]])

(defn trend-card [trend]
  (m/node `[~(keyword (str "li.trend-card#" trend))
            [:h1 ~trend]]))

(m/deftemplate trend [trend]
  [:div#content
   [:#stream]
   [:#header
    [:div#home-button
     [:h1 "HOME"]]
    [:div#title
     [:h1#visibe-title "VISIBE"]
     [:img {:src "placeholder.png" :width "170px" :height "170px" :margin-bottom "50px"}]
     [:h1#trend-title trend]
     [:div#intro]]]

   [:ul#feed]])

(defn datum-card [{text :text user :user created-at :created-at
                   name :name screen-name :screen-name
                   profile-image-url-https :profile-image-url-https}]
  
  (m/node [:li.feed-datum [:div.tweet `[:ul [:li [:img {:src ~profile-image-url-https :width "100px" :height "100px"}]]
                                        [:li [:h1 "text" ~text]]
                                        [:li [:h1 "user" ~user]]
                                        [:li [:h1 "created-at" ~created-at]]
                                        [:li [:h1 "name" ~name]]
                                        [:li [:h1 "screen-name" ~screen-name]]]]]))
