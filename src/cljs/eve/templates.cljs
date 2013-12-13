(ns eve.templates
  "Raw dommy templates"
  (:require [dommy.core :as dommy]
            [shodan.console :as console]
            [eve.utils :refer [->slug]]
            [cljs.core.match]
            [cljs-time.coerce :as coerce]
            [cljs-time.core :as c]
            [cljs-time.local]
            [dommy.utils :as utils]
            [goog.i18n.TimeZone :as tz]
            [goog.date.DateTime :as dt])
  (:require-macros [cljs.core.match.macros :refer [match]]
                   [dommy.macros :as m :refer [sel1 sel deftemplate]]))

(def instagram-default-profile-photo "this.setAttribute(\"src\", \"http://d22r54gnmuhwmk.cloudfront.net/photos/8/ih/ln/GuiHlNPfszVeNBo-556x313-noPad.jpg\")")
(def twitter-default-profile-photo "this.setAttribute(\"src\", \"https://abs.twimg.com/sticky/default_profile_images/default_profile_4_normal.png\")")
(def remove-node "this.parentNode.removeChild(this)")

;;; FIXME, Tue Dec 10 2013, Francis Wolke
;;; This code is supposed to be used for turning tweets into links - but because
;;; twitter hands back incorrect indicies, it's difficult to know what I should
;;; do with them.  Another thought - just take the URL's and split on
;;; them. There is enough data around to ensure that it works.

;; (defn- str-section
;;   "From N to M in S, indexed from 0"
;;   [s n m] (apply str (take m (drop n (seq s)))))

;; (defn- url->link [url]
;;   [:a {:href url :target "_blank"} url])

;; (defn format-with-links
;;   "Accepts a tweet string and gives back the template to represent it, turning 
;;    any URL's in into links"
;;   [s idxs]
;;   (loop [is idxs
;;          acc [:p]]
;;     (if is
;;       (let [[n m :as i] (first is)
;;             last-i (.indexOf idxs i)
;;             last-m (if (zero? last-i) 0 (second (idxs (dec last-i))))
;;             tweet-text (str-section s last-m n)
;;             link (url->link (str-section s n m))]
;;         (recur (next is) (vec (concat acc [tweet-text] [link]))))
;;       acc)))

(defn x-time-ago [created-at]
  ;; http://docs.closure-library.googlecode.com/git/class_goog_i18n_TimeZone.html
  ;; http://google-web-toolkit.googlecode.com/svn/trunk/user/src/com/google/gwt/i18n/client/constants/TimeZoneConstants.properties
  (let [created-at (coerce/from-long created-at)
        jf (juxt c/day c/hour c/minute)
        ;; n (c/now)
        [now-days now-hours now-mins] (jf (c/now))
        [datum-days datum-hours datum-mins] (jf created-at)
        ;; Hack to deal with the fact that I don't want to create a 'correct' time implementation yet
        timezone-offset (/ (.getTimezoneOffset (js/Date.)) 60)
        
        [now-days now-hours] (if (< now-hours timezone-offset)
                               [(dec now-days) (- 24 (- timezone-offset now-hours))]
                               [now-days (- now-hours timezone-offset)])

        [datum-days datum-hours] (if (< datum-hours timezone-offset)
                                   [(dec datum-days) (- 24 (- timezone-offset datum-hours))]
                                   [datum-days (- datum-hours timezone-offset)])
        days (- now-days datum-days)
        hours (- now-hours datum-hours)
        minutes (- now-mins datum-mins)]
    [days hours minutes]
    (match [days hours minutes]
           [0 0 _] (str " " minutes " minutes ago")
           [0 _ _] (str " " hours " hours ago")
           [_ _ _] (str " " days  " days ago")
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tweets

(deftemplate tweet
  [{text :text created-at :created-at name :name screen-name :screen_name
    profile-image-url :profile_image_url_https id-str :id_str :as datum}]
  (let [twitter-profile-url (str "https://www.twitter.com/" screen-name)
        tweet-url           (str "https://www.twitter.com/" screen-name "/status/" id-str)]

    [:li.social-activity.tweet
     [:a.user-img {:href twitter-profile-url
                   :target "_blank"}
      [:img {:src profile-image-url
             :onerror twitter-default-profile-photo}]]
     [:div.content
      [:a.user-name {:href twitter-profile-url
                     :target "_blank"} name]
      [:span.byline "On " [:a {:href tweet-url
                               :target "_blank"} "Twitter"] (x-time-ago created-at)]
      [:div.body-content (format-tweet text)]]]))

(deftemplate tweet-photo [{text :text created-at :created-at name :name screen-name :screen_name
                           profile-image-url :profile_image_url_https id-str :id_str photo-url :photo-url
                           link-urls :link-urls :as datum}]

  (let [twitter-profile-url (str "https://www.twitter.com/" screen-name)
        tweet-url           (str "https://www.twitter.com/" screen-name "/status/" id-str)]

    (console/log "tweet-photo: " tweet-url " profile: " twitter-profile-url)

    [:li.social-activity.tweet
     [:a.user-img {:href (str "https://www.twitter.com/" screen-name)
                   :target "_blank"} [:img {:src profile-image-url
                                            :onerror twitter-default-profile-photo}]]
     [:div.content
      [:a.user-name {:href (str "https://www.twitter.com/" screen-name)
                     :target "_blank"} name]
      [:span.byline "On " [:a {:href (str "https://www.twitter.com/" screen-name "/status/" id-str)
                               :target "_blank"} "Twitter"] (x-time-ago created-at)]
      [:div.body-content (if link-urls
                           (format-tweet text))]
      [:img {:src photo-url :onerror remove-node}]]]))

(deftemplate video-tweet [{text :text created-at :created-at name :name screen-name :screen_name
                           profile-image-url :profile_image_url_https id-str :id_str}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Instagrams

(deftemplate instagram-photo [{tags :tags created-at :created-at type :type username :username
                               profile-picture :profile-picture text :text full-name :full-name
                               link :link url :photo}]
  [:li.social-activity.instagram
   [:a.user-img {:href (str "http://www.instagram.com/" username) :target "_blank"}
    [:img {:src profile-picture
           :onerror instagram-default-profile-photo}]]
   [:div.content
    [:a.user-name {:href (str "http://www.instagram.com/" username) :target "_blank"} full-name]
    [:span.byline "On " [:a {:href link :target "_blank"} "Instagram"] (x-time-ago created-at)]
    [:div.body-content text]
    [:div.photo [:img {:src url :onerror remove-node}]]]])

(deftemplate instagram-video [{tags :tags id :id created-at :created-at type :type username :username
                               profile-picture :profile-picture full-name :full-name link :link
                               text :text url :video}]
  [:li.social-activity.instagram
   [:a.user-img {:href (str "http://www.instagram.com/" username)
                 :target "_blank"}
    [:img {:src profile-picture
           :onerror instagram-default-profile-photo}]]
   [:div.content
    [:a.user-name {:href (str "http://www.instagram.com/" username) :target "_blank"} full-name]
    [:span.byline "On " [:a {:href link :target "_blank"} "Instagram"] (x-time-ago created-at)]
    [:div.body-content text]
    [:div.video
     `[~(keyword (str "video.instagram-video" id))
       {:width "550px" :height "550px"
        :class "video-js vjs-default-skin vjs-big-play-centered"
        :onerror remove-node
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
