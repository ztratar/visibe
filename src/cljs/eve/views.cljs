(ns ^{:doc "View logic. Ties together templates, population logic and listeners."}
  eve.views
  (:require [dommy.core :refer [listen! append! prepend! html] :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console]
            [eve.state :refer [state assoc-in-state! gis]])
  (:require-macros [dommy.macros :as m :refer [sel1]]))

;;; FIXME, Sat Oct 19 2013, Francis Wolke

;;; trends -> topics 

; Home
;*******************************************************************************

(defn home [trends]
  ;; FIXME, Thu Oct 24 2013, Francis Wolke
  ;; The home view 'knows' to grab 9 trends. This is wrong.
  (let [trend-m trends
        trends (take 9 (keys trend-m))]
    (swap-view! (t/home trends))
    (let [trends-list (m/sel1 :#trends)]
      (doseq [trend trends]
        (let [trend-node (t/trend-card trend)]
          (dommy/append! trends-list trend-node)
          (dommy/set-style! trend-node :background (str "url(" (trend-m trend) ")"))
          (dommy/listen! trend-node :click (fn [& _] (navigate! :trend trend))))))))

; Trend
;*******************************************************************************

(defn play-video [selector]
  (.play (m/sel1 selector)))

(defn pause-video [selector]
  (.pause (m/sel1 selector)))

(defn add-datum! [{type :type id :id :as datum}]
  (let [datum-card (case type
                     ;; FIXME, NOTE, Fri Oct 25 2013, Francis Wolke
                     ;; Instagram's carry the type with them by default
                     :instagram-video (t/instagram-video datum)
                     :instagram-photo (t/instagram-photo datum)
                     :vine (t/vine datum)
                     :tweet (t/tweet datum))])
  (if (= "" (sel1 :#feed))
    (append! :#feed datum-card)
    (prepend! :#feed datum-card))
  ;; NOTE, Thu Oct 24 2013, Francis Wolke
  ;; Setup pause / play functionality. We create buttons with id's based 
  ;; on the instagram datum id in the template.
  (when (= :instagram-video type)
    (dommy/listen! (sel1 (keyword (str "button#play-" id)))
                   (fn [& _] (play-video (keyword (str "button#play-" id)))))
    (dommy/listen! (sel1 (keyword (str "button#pause-" id)))
                   (fn fn [& _] (pause-video (keyword (str "button#play-" id)))))))

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

g; misc
;*******************************************************************************

(defn swap-view! [node]
  ;; TODO, Mon Oct 14 2013, Francis Wolke
  ;; Hide these instead of replacing contents? 
  (dommy/replace-contents! (m/sel1 :#content) node)) 

(defn navigate! [view & args]
  ;; TODO, Tue Oct 15 2013, Francis Wolke
  ;; Needs to update `state :view'
  ;; This has a bug, if you don't pass anything to trend, it'll work, but without a trend,
  ;; It should throw, unless the issue is me not catching it??
  (case view
    :trend (do (assoc-in-state! [:view] :trend)
               (apply trend args))
    :home (do (assoc-in-state! [:view] :home)
              (home (:trends @state)))))
