(ns ^{:doc "Logic for the trend view."}
  eve.views.trend
  (:require [dommy.core :refer [listen! append! prepend! html] :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console]
            [eve.state :refer [state assoc-in-state!]])
  (:require-macros [dommy.macros :as m :refer [sel1]]))

(defn add-datum! [{type :type :as datum}]
  (let [datum-card (case type
                     :instagram-video (t/instagram-video datum)
                     :instagram-photo (t/instagram-photo datum)
                     :vine (t/vine datum)
                     :tweet (t/tweet datum))])
  (if (= "" (sel1 :#feed))
    (append! :#feed datum-card)
    (prepend! :#feed datum-card)))

(defn add-new-datums!
  "Adds undisplayed datums to feed"
  []
  (let [datums (:datums @state)
        new-datums (take-while #(not= (gis [:last-datum])))]
    (doseq [datum ]
      (add-datum! datum))))

;; Video stuff

(defn play-video [selector]
  (.play (m/sel1 selector)))

(defn pause-video [selector]
  (.pause (m/sel1 selector)))

(defn trend [trend]
  (swap-view! (t/trend trend ((:trends @state) trend)))
  (add-new-datums!)
  (dommy/listen! (m/sel1 :#home-button) :click (fn [& _] (navigate! :home))))

(defn feed-update! [key identify old new]
  (case (:view @state)
    :trend (add-new-datums!)
    (console/log "Feed update NoOp")))
