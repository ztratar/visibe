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
     ~@(map (fn [trend] [:li.trend-card trend]) (:trends @state))]])

(m/deftemplate trend [trend]
  [:div#content
   [:div#title
    [:h1 "Visibe"]
    [:div.button#home]
    [:h1 "Picture of trend here"]
    [:h1#trend-title trend]]
   [:div#feed
    [:h1 "feed goes here."]]])

(defn navigate! [view & args]
  (swap-view! (case view
                :trend (apply trend args)
                :home (home))))

(defn ^:export bootstrap! []
  (update-current-trends!)
  (navigate! :home))
