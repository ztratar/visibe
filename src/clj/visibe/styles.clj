(ns ^{:doc "CSS style information associated with the application. Compiles to
    ./resources/public/css/main.css "}
  visibe.styles
  (:require [garden.core :refer [css]]))

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
   [:h1 {:font-family "Gotham-Bold"}]
   [:ul {:list-style "none"}]
   [:img {:margin-left :auto
          :margin-right :auto
          :z-index 1}]

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

   [:#title {:margin-left "auto"
             :margin-right "auto"
             :text-align "center"
             :margin-top "70px"
             :width "450px"
             :height "200px"}

    [:h2 {:margin-top "20px"
          :font-family "Sans-Serif"
          :font-weight "100"}]]

   [:#trends {:width "1300px"}]
   [:.trend-card {:width "340px"
                  :height "220px"
                  :text-align :center
                  :margin "20px"
                  :padding "55px"
                  ;; :center-top "30px"
                  :box-shadow (str "6px 7px 35px " deep-shadow)
                  :background "url(\"../breaking-bad.png\")"}

    [:h2 {:font-family "Helvetica Neue"
          :font-size "35px"
          :color "#fff"}]
    
    [:h3 {:color emphasis
          :font-family "Helvetica Neue"
          :font-size "15px"}]

    [:style {:color emphasis
             :font-family "Helvetica Neue"
             :font-size "15px"}]]

   [:.trend-card:hover {:background "red"}]       
   
   ;; Trend

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
          ;; :margin-
          ;; :margin-right
          :text-align :center
          :font-size "25px"}]]

   [:#visibe-title {:font-size "50px"
                    :margin-bottom "50px"
                    :font-family "Gotham-Bold"}]

   [:#trend-title {:font-size "40px"
                   :background-color "rgba(0,0,0,0.0)"
                   :margin-top "30px"
                   :font-family "Gotham-Bold"}]

   [:.circle {:fill fill}]
   [:.rect {:fill fill}]
   
   [:#stream {:height "100%"
              :background timeline-emphasis
              :position :fixed
              :left "50%"
              :z-index "-1"
              ;; :margin-top "200px"
              :margin-left "-5px"
              :width "10px"}]))

(spit (java.io.File. "./resources/public/css/style.css") css-data)
     
