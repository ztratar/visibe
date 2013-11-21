(ns ^{:doc "Raw dommy templates"}
  eve.templates
  (:require [dommy.core :as dommy]
            [shodan.console :as console]
            [eve.utils :refer [->slug]]
            [cljs-time.coerce :as coerce]
            [cljs-time.core :as c]
            [dommy.utils :as utils])
  (:require-macros [dommy.macros :as m :refer [deftemplate]]))

(defn x-time-ago [created-at]
  (let [created-at      (coerce/from-long created-at)
        now             (c/now)
        now-hour        (c/hour now) 
        now-min         (c/minute now)
        now-days        (c/day now)
        datum-hour      (c/hour created-at)
        datum-min       (c/minute created-at)
        datum-days      (c/day created-at)
        day-difference  (- now-days datum-days)
        hour-difference (- now-hour datum-hour)
        min-difference  (- now-hour datum-min)]

    (cond (not= 0 day-difference)  (str " " day-difference " days ago")
          (not= 0 hour-difference) (str " " hour-difference " hours ago")
          :else (str " " min-difference " minutes ago"))))

(deftemplate ^{:doc "Generates a template for the supplied data structure"}
  automagic
  [hashmap-of-some-sort]
  [:div.automagic-template [:p (str hashmap-of-some-sort)]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Home

(defn trend-card [trend]
  (m/node `[~(keyword (str "li.trend-card#" trend))
            [:a {:href ~(str "/" (->slug trend))}
             [:div.name-container
              [:h2.trend-card-title ~trend]]
             [:span]]]))

(deftemplate home []
  [:div#content
   [:div#title
    [:h1 "Visibe"]
    [:p.description "Watch situations unfold as they happen"]]
   [:div.container.trends-container
    [:ul#trends.clearfix]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Trend

(deftemplate tweet [{text :text created-at :created-at
                     name :name screen-name :screen-name
                     profile-image-url :profile_image_url_https}]
 
  [:li.social-activity.tweet
   [:a.user-img {:href "#"} [:img {:src profile-image-url}]]
   [:div.content
    [:a.user-name {:href "#"} name]
    [:span.byline "On " [:a {:href "#"} "Twitter"] (x-time-ago created-at)]
    [:div.body-content text]]])

(deftemplate instagram-photo [{tags :tags created-at :created-at type :type
                               username :username profile-picture :profile-picture
                               text :text full-name :full-name link :link
                               {height :height url :url width :width} :photo}]
  [:li.social-activity.instagram
   [:a.user-img {:href "#"} [:img {:src profile-picture}]]
   [:div.content
    [:a.user-name {:href "#"} full-name]
    [:span.byline "On " [:a {:href "#"} "Instagram"] (x-time-ago created-at)]
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
    [:span.byline "On " [:a {:href "#"} "Instagram"] (x-time-ago created-at)]
    [:div.body-content text]
    [:div.video
     `[~(keyword (str "video.instagram-video" id))
       {:width "550px" :height "550px"
        :class "video-js vjs-default-skin vjs-big-play-centered"
        :controls "true"
        :preload "auto"}
       [:source {:src ~url :type "video/mp4"}]]]]])

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
   [:div#feed.social-feed 
    [:div.line]
    [:div#feed-left]
    [:div#feed-right]
    [:div.loader [:img {:src "/img/ajax-loader.gif"}]]]])
