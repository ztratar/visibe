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

  [:div.datum.tweet
   [:div.profile-pic [:img {:src profile-image-url-https}]
    [:div.tweet-block
     [:ul
      [:li [:h3.tweet-name name]]
      [:li [:h4.datum-metadata "On" [:i "Twitter"] " 3 minutes  ago"]]
      [:br]
      [:li [:h2.tweet-text text]]]]]])

(deftemplate instagram-photo [{tags :tags created-at :created-at type :type
                               username :username profile-picture :profile-picture
                               full-name :full-name link :link
                               {height :height url :url width :width} :photo}]

  [:div.instagram-photo-card
   [:img.profile-pic {:src profile-picture}]
   [:ul.instagram-datum
    [:li [:h3.datum-name full-name]]
    [:li [:h4.datum-metadata "On " [:i "Instagram"] " 3 minutes ago"]]
    [:li.tweet-text "Photo whatever"]]
   [:div.instagram-photo
    [:img {:src url}]]]) 

(deftemplate instagram-video [{tags :tags id :id created-at :created-at
                               type :type username :username
                               profile-picture :profile-picture
                               full-name :full-name link :link
                               {height :height url :url width :width} :video}]

  [:div.instagram-video-card
   [:img.profile-pic {:src profile-picture}]
   [:ul.instagram-datum
    [:li [:h3.datum-name full-name]]
    [:li [:h4.datum-metadata "On " [:i "Instagram"] " 3 minutes ago"]]
    [:li.tweet-text "The first time that I had nutella"]]
   ;; TODO, Wed Nov 06 2013, Francis Wolke
   ;; Is the custom ID even neccecary?
   `[~(keyword (str "video.instagram-video" id))
     {:width "550px" :height "550px"
      :class "video-js vjs-default-skin vjs-big-play-centered"
      :controls "true"
      :preload "auto"}
     [:source {:src ~url :type "video/mp4"}]]])

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
     [:img.trend-img {:src (str "http://localhost:9000/cropped-images/" image-url)
                      :width "170px" :height "170px"}]
     [:h1#trend-title trend]]]
   [:ul#feed]])

;; (deftemplate datum-share-buttons [datum-url]
;;   [:div.datum-share-buttons
;;    ;; Twitter
;;    [:a.twitter-share-button {:href "https://twitter.com/share"
;;                              :data-url datum-url
;;                              :target "_blank"} "Tweet"]
;;    [:script "!function(d,s,id){var    js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document,
;;     'script', 'twitter-wjs');"]
;;    ;; G+
   
;;    ;; Facebook
;;    ])

;; (append! (sel1 :#feed)
;;          (m/node [:div {:class "g-plus"
;;                         :data-action "share"
;;                         :data-height 24
;;                         :data-href "http://www.visibe.com"}]))

;; (append! (sel1 :#feed)
;;          (m/node [:script {:type "text/javascript"}
;;                   "(function() {
;;     var po = document.createElement('script'); po.type = 'text/po; javascript't.async = true;
;;     po.src = 'https://apis.google.com/js/plusone.js';
;;     var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
;;   })();"]))
