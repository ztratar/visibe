(ns ^{:doc "Raw dommy templates"}
  eve.templates
  (:require [dommy.core :as dommy]
            [shodan.console :as console]
            [eve.utils :refer [->slug]]
            [cljs.core.match]
            [cljs-time.coerce :as coerce]
            [cljs-time.core :as c]
            [dommy.utils :as utils])
  (:require-macros [cljs.core.match.macros :refer [match]]
                   [dommy.macros :as m :refer [deftemplate]]))

(defn x-time-ago [created-at]
  ;; TODO, Thu Nov 21 2013, Francis Wolke
  ;; The issue here is time zones - I hope
  (let [created-at      (coerce/from-long created-at)
        now             (c/now)

        now-min         (c/minute now)
        now-hour        (c/hour now) 
        now-days        (c/day now)

        datum-min  (c/minute created-at)
        datum-hour (c/hour created-at)
        datum-days (c/day created-at)

        minutes    (- now-min datum-min)
        hours      (- now-hour datum-hour)
        days       (- now-days datum-days)]

    (console/log "time: " created-at " time zone: " (.getTimezoneOffset created-at))

    (match [days hours mins]
           [0 0 _] (str " " days  " minutes ago")
           [0 _ _] (str " " hours " hours ago")
           [_ _ _] (str " " minutes  " days ago")
           :else (console/error "x-time-ago received bad input"))))

(defn format-tweet
  "Accepts a tweet string and gives back the template to represent it, taking
   into account any URLs that exist"
  [s]
  (let [url  (first (re-find #"(http|ftp|https)://[\w-]+(\.[\w-]+)+([\w.,@?^=%&amp;:/~+#-]*[\w@?^=%&amp;/~+#-])?" s))
        link (when url [:a {:href url :target "_blank"} url])
        text (when url (clojure.string/split s url))]
    (cond (empty? url)        [:p s]
          (= 1 (count text))  [:p (first text) link]
          (= "" (first text)) [:p link (second text)]
          (= 2 (count text))  [:p (first text) link (second text)]
          ;; TODO, Thu Nov 21 2013, Francis Wolke
          ;; Multiple links within a tweet
          :else               (do (console/error "We currently don't handle multiple links within a tweet")
                                  [:p s]))))

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

(deftemplate tweet [{text :text created-at :created-at name :name screen-name :screen_name
                     profile-image-url :profile_image_url_https id-str :id_str}]
 
  [:li.social-activity.tweet
   [:a.user-img {:href (str "https://www.twitter.com/" screen-name)
                 :target "_blank"} [:img {:src profile-image-url}]]
   [:div.content
    [:a.user-name {:href (str "https://www.twitter.com/" screen-name)
                   :target "_blank"} name]
    [:span.byline "On " [:a {:href (str "https://www.twitter.com/" screen-name "/status/" id-str)
                             :target "_blank"} "Twitter"] (x-time-ago created-at)]
    [:div.body-content (format-tweet text)]]])

(deftemplate instagram-photo [{tags :tags created-at :created-at type :type username :username
                               profile-picture :profile-picture text :text full-name :full-name
                               link :link {height :height url :url width :width} :photo}]
  [:li.social-activity.instagram
   [:a.user-img {:href (str "http://www.instagram.com/" username) :target "_blank"} [:img {:src profile-picture}]]
   [:div.content
    [:a.user-name {:href (str "http://www.instagram.com/" username) :target "_blank"} full-name]
    [:span.byline "On " [:a {:href link :target "_blank"} "Instagram"] (x-time-ago created-at)]
    [:div.body-content text]
    [:div.photo [:img {:src url}]]]])

(deftemplate instagram-video [{tags :tags id :id created-at :created-at type :type username :username
                               profile-picture :profile-picture full-name :full-name link :link
                               text :text {height :height url :url width :width} :video}]
  [:li.social-activity.instagram
   [:a.user-img {:href (str "http://www.instagram.com/" username)
                 :target "_blank"} [:img {:src profile-picture}]]
   [:div.content
    [:a.user-name {:href (str "http://www.instagram.com/" username) :target "_blank"} full-name]
    [:span.byline "On " [:a {:href link :target "_blank"} "Instagram"] (x-time-ago created-at)]
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
