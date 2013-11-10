(ns ^{:doc "CSS style information. Compiles to ./resources/public/css/main.css "}
  visibe.styles
  (:refer-clojure :exclude [+ - * /])
  (:require [garden.core :refer [css]]
            [garden.arithmetic :refer [+ - * /]]))

;;; TODO, Thu Oct 17 2013, Francis Wolke
;;; Almost every thing in here needs to be renamed.
;;; Garden has the concept of units, which will clean up this code.

;;; NOTE, Wed Nov 06 2013, Francis Wolke
;;; Hook up the color pallet to a lazy seq of pallets from color lovers.
;;; Have emacs save this file after swapping out the colors on command,
;;; Allowing a designer to explore the color schemes, then at their wish,
;;; move onto another, already hand picked.

;;; Pallet

(def shadow "#245d82")
(def deep-shadow "#183144")
(def timeline-emphasis "#5b8fb5")
(def fill "#3e80ab")
(def emphasis "#9bb3c8")
(def text-color "#fff")
(def background "#286d99")

(def font0 "Gotham-Bold")
(def font1 "Sans-Serif")
(def font2 "Helvetica Neue")
(def font3 "Gotham")

(def css-data
  (css {:pretty-print? true}
       ;; Common
   
       [:* {:margin 0 :padding 0
            :color text-color :background background
            :font-family "Gotham-Bold"}]
       [:h1 {:background-color  "rgba(0,0,0,0.0)"}]
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

       [:.axis :path
        :.axis :line
        {:fill "none" :stroke "#000" :shape-rendering "crispEdges"}]
       [:.brush :.extent {:stroke "#fff"
                          :fill-opacity 0.125
                          :shape-rendering "crispEdges"}]

       ;; Home

       [:#title {:margin-left :auto
                 :margin-right :auto
                 :text-align "center"
                 :margin-top "70px"
                 :width "450px"
                 :height "200px"}

        [:h2 {:margin-top "20px"
              :font-family "Sans-Serif"
              :font-weight "100"}]]

       [:#trends {:width "1260px"
                  :margin-left :auto
                  :margin-right :auto }]

       [:.trend-card {:width "300px"
                      :height "180px"
                      :vertical-align "top"
                      :opacity "0.8"
                      :text-align :center
                      :margin "10px"
                      :display :inline-block
                      :padding "50px"
                      :box-shadow (str "6px 7px 35px " deep-shadow)}

        [:style {:color emphasis
                 :font-family "Helvetica Neue"
                 :font-size "15px"}]]

       [:h3 {:color emphasis
             :font-family "Helvetica Neue"
             :font-size "15px"}]

       [:h2 {:font-family "Helvetica Neue"
             :font-size "35px"
             :color "#fff"}]

       [:.trend-card-title {:font-family "Helvetica Neue"
                            :opacity "1.0"
                            :font-size "35px"
                            :margin-top "50px"}]

       ;; Trend View

       [:#header {:width "100%"
                  :height "300px"}]

       [:.trend-img {:border-width "20px"
                     :margin-bottom "50px"
                     :border-radius "50%"
                     :background-color "rgba(0,0,0,0.0)"
                     :border-color timeline-emphasis}]

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

       ;; Datum cards

       [:.datum-metadata {:margin-top "7px"
                          :color timeline-emphasis
                          :font-family font0
                          :font-size "14px"}
        [:i {:margin-top "7px"
             :color timeline-emphasis
             :font-family font0
             :font-size "14px"}]]

       [:.datum-name {:color "#fff"
                      :font-size "16px"
                      :font-family font0}]

       [:.profile-pic {:border-radius "50px" :width "100px" :height "100px"}]
       
       ;; Tweet Card

       [:.datum.tweet {:margin-left "50%"
                       :margin-right "50%"
                       :position :absolute
                       :background "rgba(0,0,0,0.0)"}]

       [:.tweet-block {:margin-left "85px"
                       :margin-top "-50px"
                       :position :relative}]

       [:.tweet-name {:color "#fff"
                      :font-size "16px"
                      :font-family font0}]

       [:.tweet-text {:color "#fff"
                      :font-size "16px"
                      :font-family font3
                      :word-wrap :break-word
                      :width "400px"}]

       ;; Instagram video card

       [:.instagram-datum {:margin-left "120px"
                           :margin-top "-100px"
                           :position :relative
                           :background "rgba(0,0,0,0.0)"}]

       [:.instagram-video-card {:margin-left "50%"
                                :margin-right "50%"
                                :position :absolute}]

       [:.instagram-video {:margin-left "120px"
                           :margin-top "10px"}]
       ))

(spit (java.io.File. "./resources/public/css/style.css") css-data)
