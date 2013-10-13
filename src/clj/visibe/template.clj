(ns visibe.template
  (:use hiccup.core hiccup.page))

(defn index []
  (html5 [:head
          [:title "Visibe - Watch situations and reactions unfold as they happen"]
          [:meta {:charset "UTF-8"}]
          (include-css "css/fonts.css" "css/style.css")]

         [:body (include-js "js/libs/d3.v3.min.js" "js/visibe_dbg.js")
          [:div#content
            [:img {:src "breaking-bad.png"}]
           [:script {:type "text/javascript"} "eve.core.bootstrap_BANG_();"]]]))

