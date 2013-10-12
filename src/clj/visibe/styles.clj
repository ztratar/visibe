(ns ^{:doc "CSS style information associated with the application. Compiles to
    ./resources/public/css/main.css "}
  visibe.styles
  (:require [garden.core :refer [css]]))

(def major "#01D2FF")
(def minor "#79C7EB")
(def emphasis "#F8DAFB")
(def text-c "#CDEFFF")
(def background "#07060A")

(def css-data
  (css
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
   
   [:* {:margin 0 :padding 0 :text-color text-c}]
   
   ;; [:#actionbar {:position "absolute"
   ;;               :top 0
   ;;               :height "35px"
   ;;               :width "100%"
   ;;               :background minor
   ;;               :border-bottom-style "solid"
   ;;               :border-bottom-width "1px"
   ;;               :border-bottom-color emphasis}
    
   ;;  [:ul {:height "60px"
   ;;        :width "100%"}]
    
   ;;  [:li {:display :inline
   ;;        :float :right
   ;;        :list-style-type :none
   ;;        :position :relative}]
    
   ;;  [:a :.menubtn {:display :block
   ;;                 :width "100px"
   ;;                 :background minor
   ;;                 :height "35px"}]

   ;;  [:a:hover :.menubtn:hover {:background major}]
   ;;  ]

   ;; ;; Profile
               
   ;; [:#profile {:position :relative
   ;;             :margin-left :auto
   ;;             :margin-right :auto
   ;;             :width 800
   
   ;;             :height 500
   ;;             :background major}]

   ;; [:#login-button {:width 200
   ;;                  :height 200
   ;;                  :background "red"
   ;;                  :position "relative"}]
               
   ;; [:#login {:position :absolute
   ;;           :width "400px"
   ;;           :height "400px"
   ;;           :color major
   ;;           :margin-left :auto
   ;;           :margin-right :auto}]
   ))

(spit (java.io.File. "./resources/public/css/main.css") css-data)
     
