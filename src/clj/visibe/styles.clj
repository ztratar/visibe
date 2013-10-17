(ns ^{:doc "CSS style information associated with the application. Compiles to
    ./resources/public/css/main.css "}
  visibe.styles
  (:require [garden.core :refer [css]]))

;;; TODO, Thu Oct 17 2013, Francis Wolke

;;; Almost every thing in here needs to be renamed.

;;; Garden has the concept of units, which will clean up this code.


(def shadow "#245d82")
(def deep-shadow "#183144")
(def timeline-emphasis "#5b8fb5")
(def fill "#3e80ab")
(def emphasis "#9bb3c8")
(def text-color "#fff")
(def background "#286d99")

(def css-data
  (css
   ;; Common
   
   [:* {:margin 0 :padding 0
        :color text-color :background background
        :font-family "Gotham-Bold"}]
   [:h1 :background-color]
   [:ul {:list-style "none"}]
   [:img {:margin-left :auto
          :margin-right :auto
          :z-index 1}]

   [:div {:background-color "rgba(0,0,0,0.0)"}]
   [:#content {:background-color "rgba(0,0,0,0.0)"}]
   [:body     {:background-color "rgba(0,0,0,0.0)"}]

   ;; D3
   [:svg {:font "10px sans-serif"}]
   [:path {:fill "steelblue"
           :background "steelblue"}]

   ;; [:.axis :path
   ;;  :.axis :line
   ;;  {:fill "none" :stroke "#000" :shape-rendering "crisp-edges"}]
   ;; [:.brush :.extent {:stroke "#fff"
   ;;                    :fill-opacity 0.125
   ;;                    :shape-rendering "crisp-edges"}]

   ;; Home

   [:#title {:margin-left :auto
             :margin-right :auto
             :text-align "center"
             :margin-top "70px"
             :width "450px"
             :height "200px"
             }

    [:h2 {:margin-top "20px"
          :font-family "Sans-Serif"
          :font-weight "100"}]]

   [:#trends {:width "1260px"
              :margin-left :auto
              :margin-right :auto }]

   [:.trend-card {:width "300px"
                  :height "180px"
                  :vertical-align "top"
                  :text-align :center
                  :background-color "rgba(0,0,0,0.0)"
                  :margin "10px"
                  :display :inline-block
                  :padding "50px"
                  :box-shadow (str "6px 7px 35px " deep-shadow)}

    [:h2 {:font-family "Helvetica Neue"
          :font-size "35px"
          :color "#fff"}]
    
    
    [:h3 {:color emphasis
          :font-family "Helvetica Neue"
          :font-size "15px"}]

    [:style {:color emphasis
             :font-family "Helvetica Neue"
             :font-size "15px"}]
    [:li  {:background-color "rgba(0,0,0,0.0)"}]]

   [:.trend-card-title {:font-family "Helvetica Neue"
                        :font-size "35px"
                        :margin-top "50px"}]

   ;; Trend View

   [:#header {:width "100%"
              :height "300px"}]

   [:#home-button {:width "146px"
                   :height "70px"
                   :border-radius "10px"
                   :background deep-shadow
                   :position :absolute
                   :margin-top 0
                   :margin-left "60px"}

    [:h1 {:background-color "rgba(0,0,0,0.0)"
          :color emphasis
          :margin-top "24px"
          :text-align :center
          :font-size "25px"}]]

   [:#visibe-title {:font-size "50px"
                    :margin-bottom "30px"
                    :font-family "Gotham-Bold"}]

   [:#trend-title {:font-size "40px"
                   :background-color "rgba(0,0,0,0.0)"
                   :margin-top "-20px"
                   :font-family "Gotham-Bold"}]

   [:.circle {:fill fill}]
   [:.rect {:fill fill}]
   
   [:#stream {:height "100%"
              :background timeline-emphasis
              :position :absolute
              :left "50%"
              :z-index "-1"
              :margin-top "200px"
              :margin-left "-5px"
              :width "10px"}]

   ;; Datum card

   [:.datum {:magin-left :auto
             :magin-right :auto}]

   [:h4 {:font-size "20px"}]
   [:h5 {:font-size "20px"}]

   ;; [:.feed-datum {:width "450px"
   ;;                :height "200px"
   ;;                :background "red"}]

   ;; ;; [:.datum-profile-img ;; {:margin-left "60px"
   ;; ;;  ;;  :margin-top "-40px"}
   ;; ;;  ]
   ;; ;; [:.tweet {:margin-left "60px"
   ;; ;;           :margin-top "-40px"}]

   ;; [:.datum-info  {:margin-left "80px"
   ;;                 ;; :margin-top "0px"
   ;;                 :background "rgba(0,0,0,0.0)"}]

   ;; #_[:.datum-seperator

   ;;    [:.datum-seperator-rect {:height "10px" :width "25px"
   ;;                             :background timeline-emphasis}]

   ;;    [:.datum-seperator-circle {:width "30px" :height "30px"
   ;;                               :border-radius "15px"
   ;;                               :background timeline-emphasis
   ;;                               :margin-left "25px"
   ;;                               :margin-top "20px"}]

   ;;    ;; [:datum-card {:width }]
   ;;    ]
   
   ))

(spit (java.io.File. "./resources/public/css/style.css") css-data)
