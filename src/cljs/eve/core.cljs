(ns eve.core
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [cljs.reader :as r]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [eve.state :refer [state assoc-in-state!]]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :as am]
                   [dommy.macros :as m]))

; Misc
;*******************************************************************************

(repl/connect "http://localhost:8002/repl")

(defn printc [& m]
  (.log js/console (apply str m)))

(defn process-socket-data [data]
  (printc (.-data data)))

(defn ws-connect []
  (let [ws (js/WebSocket. "ws://localhost:9000/ws")
        _ (set! (.-onerror ws) #(printc "Websocket Error: " %))
        _ (set! (.-onmessage ws) process-socket-data)]
    (assoc-in-state! [:websocket-connection] ws)))

(defn update-current-trends! []
  (am/go (let [response (<! (http/post "http://localhost:9000/api/current/trends"
                                       {:headers {"content-type" "application/data"}}))]
           (assoc-in-state! [:trends] (r/read-string (:body response))))))

(defn route->fn-name [sym]
  (clojure.string/replace (str sym) "/" "-"))

; Navigation + templates
;*******************************************************************************

;; (defn calc-metrics [trend]
;;   [:p.metrics  [:style "Posts "] (str (* 1000 (rand-int 30))) "IN" [:style (str (rand-int 10) "M")]])

(defn swap-view! [node]
  (dommy/replace! (m/sel1 :#content) node))

(m/deftemplate home []
  [:div#content
   [:div#title
    [:h1 "Visibe"]
    [:h2 "Watch social trends unfold in real-time"]]
   `[:ul#trends
     ;; FIXME, Fri Oct 11 2013, Francis Wolke
     ;; If trends have not been pulled down yet, then have a watch on the atom
     ;; That updates them?

     ;; Also, these things should be links, not :p's
     ~@(map (fn [trend] [:li.trend-card
                         [:h2 trend]
                         #_(calc-metrics trend)]) (:trends @state))]])

(m/deftemplate trend [trend]
  [:div#content
   [:#header 
    [:div#title
     [:h1 "Visibe"]
     [:div.button#home [:p "Home"]]
     [:h1 "Picture of trend here"]
     [:h1#trend-title trend]]]
   [:div#feed
    [:h1 "feed goes here."]]])

(defn navigate! [view & args]
  (swap-view! (case view
                :trend (apply trend args)
                :home (home))))

(defn ^:export bootstrap! []
  (update-current-trends!)
  (navigate! :home))
; D3
;*******************************************************************************

(def d3 js/d3)
(def svg  (-> d3
              (.select "body")
              (.append "svg")
              (.attr "width" "100%")
              (.attr "height" "100%")))

(defn clear! []
  (.remove (.selectAll svg "circle"))
  (.remove (.selectAll svg "image")))

(defn add-image-circle
  "Adds the image circle to the DOM and"
  [url]
  (-> svg
      ;; Add circle
      (.append "circle")
      (.attr "cx" 0)
      (.attr "cy" 0)
      (.attr "r" 0)
      (.attr "opacity" 0)
      (.attr "class" "circle")
      
      ;; Add image 
      (.append "image")
      (.attr "xlink:href" "http://localhost:9000/test_img.png")
      (.attr "x" 60)
      (.attr "y" 60)
      (.attr "width" 100)
      (.attr "height" 100))
  ;; Transitions
  (-> d3
      (.selectAll "circle")
      (.transition)
      (.duration 3000)
      (.attr "cx" 100)
      (.attr "cy" 100)
      (.attr "r" 100)
      (.attr "opacity" 1)
      (.ease "gradual")))

(defn add-feed-path []
  (-> svg
      (.append "rect")
      (.attr "fill" "#3e80ab")
      (.attr "x" "300px")
      (.attr "y" 0)
      (.attr "width" 30)
      (.attr "height" "100%")))
