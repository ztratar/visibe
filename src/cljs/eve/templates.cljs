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

(deftemplate tweet [{text :text user :user created-at :created-at
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

(deftemplate instagram-photo [{{{url :url} :standard_resolution} :images
                               created-time :created_time
                               {username :username profile-pic :profile_picture
                                name :full_name} :user tags :tags}]
  [:div.instagram-photo-card
   [:img.profile {:src profile-pic :style {:width "50px" :height "50px"}}]
   [:ul
    [:li (str "X whatever ago" created-time)]
    [:li (str "username" username)]
    [:li (str "actual name" name)]
    [:li (str "tags" (map #(str "#" %) tags))]]])

(deftemplate instagram-video [{id :id}]
  ;; Assign a custom ID that only it knows
  [:div.datum-card
   [:video {:width "400px" :height "400px"}
    [:source {:src "http://distilleryimage11.s3.amazonaws.com/5ec92b043ad411e3bc9822000ab78150_101.mp4":type "video/mp4"} ]]
   `[:ul
     [:li [~(keyword (str "button#play-" id))]]
     [:li [~(keyword (str "button#pause-" id))]]]])

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
