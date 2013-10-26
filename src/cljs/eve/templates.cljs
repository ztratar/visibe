(ns ^{:doc "Raw dommy templates"}
  eve.templates
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils])
  (:require-macros [dommy.macros :as m :refer [deftemplate]]))

;;; TODO, Thu Oct 17 2013, Francis Wolke
;;; Move style information into styles.clj

; Home
;*******************************************************************************

(defn trend-card [trend]
  (m/node `[~(keyword (str "li.trend-card#" trend))
            [:h1.trend-card-title ~trend]]))

(deftemplate home [trends]
  [:div#content
   [:div#title
    [:h1 "Visibe"]
    [:h2 "Watch social trends unfold in real-time"]]
   [:ul#trends]])

; Trend
;*******************************************************************************

(deftemplate tweet [{text :text user :user created-at :created_time
                     name :name screen-name :screen-name
                     profile-image-url-https :profile-image-url-https}]

  [:table.datum {:border "0" :background "rgba(0,0,0,0.0)"}
   [:tr
    [:td [:img.nub {:src "datum-node.png"
                    :style {:background "rgba(0,0,0,0.0)"}}] [:td]]
    [:td [:img.profile {:src "https://si0.twimg.com/profile_images/2622165696/o20xkpll5fr57alshtnd_normal.jpeg"
                        :style {:background "rgba(0,0,0,0.0)"
                                :width "50px" :height "50px"
                                :border-radius "25px"}}] [:td]]
    [:td [:h3 "Zach Tratar"] [:td [:ul
                                   [:li [:h3 "text " text]]
                                   [:li [:h4 (str "On " "twitter at " created-at)]]]]]]])

(deftemplate instagram-photo [{link :link created-at :created_time tags :tags
                               {username :username profile-pic
                                :profile-picture name :full_name} :user
                               {{instagram-url :url} :standard_resolution} :images}]
  [:div.instagram-photo-card
   [:img.profile {:src profile-pic :style {:width "50px" :height "50px"}}]
   [:img.instagram-photo {:src instagram-url :style {:width "500px" :height "500px"}}]
   [:ul
    [:li (str "X whatever ago" created-at)]
    [:li (str "username" username)]
    [:li (str "link" link)]
    [:li (str "actual name" name)]
    [:li (str "tags" (map #(str "#" %) tags))]]])

(deftemplate instagram-video [{link :link tags :tags id :id created-at :created-at
                               username :username profile-pic :profile-picture
                               poster-image-url :poster-image-url
                               video-url :video-url
                               name :full-name}]
  [:div.instagram-video-card
   `[~(keyword (str "video#" id)) {:width "550px" :height "550px"
                                   :class "video-js vjs-default-skin vjs-big-play-centered"
                                   :controls "true"
                                   :preload "auto"
                                   :poster ~poster-image-url}

     [:source {:src ~video-url :type "video/mp4"}]]
   [:img.profile {:src profile-pic :style {:width "50px" :height "50px"}}]
   [:ul
    [:li "tags" (map (partial str "#") tags)]
    [:li "link" link]
    [:li "name" name]
    [:li "username" username]
    [:li "created-at" created-at]]])

(deftemplate vine [datum]
  [:div.datum-card [:p "implement me!"]])

(deftemplate trend [trend image-url]
  [:div#content
   [:#stream]
   [:#header
    [:div#home-button
     [:h1 "HOME"]]
    [:div#title
     [:h1#visibe-title "VISIBE"]
     [:img {:src image-url
            :width "170px" :height "170px"
            :style {:margin-bottom "50px" :border-radius "85px"
                    :background-color "rgba(0,0,0,0.0)"}}]
     [:h1#trend-title trend]]]
   [:ul#feed]])
