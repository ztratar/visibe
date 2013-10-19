(ns ^{:doc "Raw dommy templates"}
  eve.templates
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils])
  (:require-macros [dommy.macros :as m]))

;;; TODO, Thu Oct 17 2013, Francis Wolke
;;; Move style information into styles.clj

; Home
;*******************************************************************************

(defn trend-card [trend background-url]
  (m/node `[~(keyword (str "li.trend-card#" trend))
            [:h1.trend-card-title ~trend]]))

(m/deftemplate home [trends]
  [:div#content
   [:div#title
    [:h1 "Visibe"]
    [:h2 "Watch social trends unfold in real-time"]]
   [:ul#trends]])

; Trend
;*******************************************************************************

(m/deftemplate datum-card [{text :text user :user created-at :created-at
                            name :name screen-name :screen-name
                            profile-image-url-https :profile-image-url-https}]

  [:table.datum {:border "0"
                 :background "rgba(0,0,0,0.0)"}
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

(m/deftemplate trend [trend]
  [:div#content
   [:#stream]
   [:#header
    [:div#home-button
     [:h1 "HOME"]]
    [:div#title
     [:h1#visibe-title "VISIBE"]
     [:img {:src (get-in @state [:trends trends :img-uri]) :width "170px" :height "170px"
            :style {:margin-bottom "50px" :border-radius "85px"
                    :background-color "rgba(0,0,0,0.0)"}}]
     [:h1#trend-title trend]]]
   [:ul#feed]])


