(ns ^{:doc "View logic. Ties together templates, population logic and listeners."}
  eve.views
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console]
            [eve.state :refer [state assoc-in-state!]])
  (:require-macros [dommy.macros :as m]))

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
!
; misc
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
