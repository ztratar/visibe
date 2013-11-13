(ns ^{:doc "Raw dommy templates"}
  eve.templates
  (:require [dommy.core :as dommy]
            [eve.utils :refer [->slug]]
            [dommy.utils :as utils])
  (:require-macros [dommy.macros :as m :refer [deftemplate]]))

; Misc
;*******************************************************************************

(deftemplate ^{:doc "Generates a template for the supplied data structure"}
  automagic
  [hashmap-of-some-sort]
  [:div.automagic-template [:p (str hashmap-of-some-sort)]])

; Home
;******************************************************************************* 

(defn trend-card [trend]
  (m/node `[~(keyword (str "li.trend-card#" trend))
            [:a {:href ~(str "/" (->slug trend))}
              [:div.name-container
                [:h2.trend-card-title ~trend]]
              [:span]]]))

(deftemplate home [trends]
  [:div#content
   [:div#title
    [:h1 "Visibe"]
    [:p.description "Watch situations unfold as they happen"]]
   [:div.container.trends-container
    [:ul#trends.clearfix]]])

; Trend
;*******************************************************************************

(deftemplate tweet [{text :text created-at :created-at
                     name :name screen-name :screen-name
                     profile-image-url-https :profile-image-url-https}]
 
  [:li.social-activity.tweet
   [:a.user-img {:href "#"} [:img {:src profile-image-url-https}]]
   [:div.content
    [:a.user-name {:href "#"} name]
    [:span.byline "On " [:a {:href "#"} "Twitter"] " 3 minutes ago"]
    [:div.body-content text]]])

(deftemplate instagram-photo [{tags :tags created-at :created-at type :type
                               username :username profile-picture :profile-picture
                               text :text full-name :full-name link :link
                               {height :height url :url width :width} :photo}]
  [:li.social-activity.instagram
   [:a.user-img {:href "#"} [:img {:src profile-picture}]]
   [:div.content
    [:a.user-name {:href "#"} full-name]
    [:span.byline "On " [:a {:href "#"} "Instagram"] " 3 minutes ago"]
    [:div.body-content text]
    [:div.photo [:img {:src url}]]]])

(deftemplate instagram-video [{tags :tags id :id created-at :created-at
                               type :type username :username
                               profile-picture :profile-picture
                               full-name :full-name link :link
                               text :text {height :height url :url width :width} :video}]
  [:li.social-activity.instagram
    [:a.user-img {:href "#"} [:img {:src profile-picture}]]
    [:div.content
      [:a.user-name {:href "#"} full-name]
      [:span.byline "On " [:a {:href "#"} "Instagram"] " 3 minutes ago"]
      [:div.body-content text]
      [:div.video
       `[~(keyword (str "video.instagram-video" id))
         {:width "550px" :height "550px"
          :class "video-js vjs-default-skin vjs-big-play-centered"
          :controls "true"
          :preload "auto"}
         [:source {:src ~url :type "video/mp4"}]]
     ]
    ]])

(deftemplate vine [datum]
  [:div.datum-card [:p "implement me!"]])

(deftemplate trend [trend image-url]
  [:div#content
   [:#stream]
   [:#header
    [:a#home-button {:href "#"} [:i.fa.fa-th-large] "home"]
    [:div#title
     [:h1#visibe-title "VISIBE"]
     [:img.trend-img {:src image-url}]
     [:h1#trend-title trend]]]
   [:ul#feed.social-feed 
      [:div#feed-left]
      [:div#feed-right]]])

;; (deftemplate datum-share-buttons [datum-url]
;;   [:div.datum-share-buttons
;;    ;; Twitter
;;    [:a.twitter-share-button {:href "https://twitter.com/share"
;;                              :data-url datum-url
;;                              :target "_blank"} "Tweet"]
;;    [:script "!function(d,s,id){var    js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document,
;;     'script', 'twitter-wjs');"]])
