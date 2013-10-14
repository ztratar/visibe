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
        :color text-color :background background}]
   [:h1 {:font-family "Gotham-Bold"}]
   [:ul {:list-style "none"}]
   [:img {:margin-left :auto
          :margin-right :auto}]

   ;; D3
   [:svg {:font "10px sans-serif"}]
   [:path {:fill "steelblue"
           :background "steelblue"}]
   [:.axis :path
    :.axis :line
    {:fill "none" :stroke "#000" :shape-rendering "crisp-edges"}]
   [:.brush :.extent {:stroke "#fff"
                      :fill-opacity 0.125
                      :shape-rendering "crisp-edges"}]

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
                  :center-top "30px"
                  :box-shadow (str "6px 7px 35px " deep-shadow)
                  :background "url(\"../breaking-bad.png\")"}

    [:h2 {:font-family "Helvetica Neue"
          :font-size "35px"
          :color "#fff"}]
    
    [:h3 {:color emphasis
          :font-family "Helvetica Neue"
          :font-size "15"}]

    [:style {:color emphasis
             :font-family "Helvetica Neue"
             :font-size "15"}]]

   [:.trend-card:hover {:background "red"}]       
   
   ;; Trend

   [:#header {:width "100%"
              :height "300px"}
    
    [:.button {:width "100px"
               :height "100px"
               :background deep-shadow}]]

   [:.circle {:fill fill}]
   [:.rect {:fill fill}]

   [:#stream {:height "100%"
              :background emphasis
              :width "50px"}]
   ))

(spit (java.io.File. "./resources/public/css/style.css") css-data)
     
