(ns ^{:doc "View logic. Ties together templates, population logic and listeners."}
  eve.views
  (:require [dommy.core :refer [listen! append! prepend! html] :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console]
            [cljs.core.async :as async :refer [<! >! chan put! timeout close!]]
            [eve.state :refer [state assoc-in-state! gis]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [dommy.macros :as m :refer [sel1]]))

;;; FIXME, Sat Oct 19 2013, Francis Wolke
;;; trends -> topics

(declare swap-view!)
(declare navigate!)

; Home
;*******************************************************************************

(defn home [trends]
  (let [trend-m trends
        trends (loop [acc #{}]
                 (if (= (* 3 (quot (count trends) 3)) (count acc))
                   acc
                   (recur (conj acc (rand-nth (keys trend-m))))))]
    (swap-view! (t/home trends))
    (let [trends-list (m/sel1 :#trends)]
      (doseq [trend trends]
        (let [trend-node (t/trend-card trend)]
          (dommy/append! trends-list trend-node)
          (dommy/set-style! trend-node :background (str "url(" (trend-m trend) ")"))
          (dommy/listen! trend-node :click (fn [& _] (navigate! :trend trend))))))))

; Trend
;*******************************************************************************

(def)

(defn add-datum! [{type :type id :id :as datum}]
  (let [datum-card (case type
                     :instagram-video (t/instagram-video datum)
                     :instagram-photo (t/instagram-photo datum)
                     :vine (t/vine datum)
                     :tweet (t/tweet datum))]
    (if (= "" (sel1 :#feed))
      (append! (sel1 :#feed) datum-card)
      (prepend! (sel1 :#feed) datum-card))))

(defn add-new-datums!
  "Adds undisplayed datums to feed"
  []
  ;; XXX, TODO, Thu Oct 24 2013, Francis Wolke
  ;; I have the sneaking suspicion that this code is buggy due to the transactional
  ;; semantics of `state'. Revisit.
  (let [datums (:datums @state)
        new-datums (take-while (partial not= (gis [:last-datum])) datums)
        new-last-datum (first new-datums)]
    (doseq [datum datums]
      (add-datum! datum))
    (assoc-in-state! [:last-datum] new-last-datum)))

(defn trend [trend]
  (swap-view! (t/trend trend ((:trends @state) trend)))
  (add-new-datums!)
  (dommy/listen! (m/sel1 :#home-button) :click (fn [& _] (navigate! :home))))

(defn feed-update! [key identify old new]
  (case (:view @state)
    :trend (add-new-datums!)
    (console/log "Feed update NoOp")))

; misc
;*******************************************************************************

(defn swap-view! [node]
  ;; TODO, Mon Oct 14 2013, Francis Wolke
  ;; Hide these instead of replacing contents? 
  (dommy/replace-contents! (m/sel1 :#content) node)) 

(defn navigate! [view & args]
  ;; TODO, Tue Oct 15 2013, Francis Wolke
  ;; Buggy - if you don't pass in a trend, it'll throw. Add checks.
  (case view
    :trend (do (apply trend args)
               (assoc-in-state! [:view] :trend))
    :home (do (home (:trends @state))
              (assoc-in-state! [:view] :home))))
