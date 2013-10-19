(ns ^{:doc "View logic. Ties together templates, population logic and listeners."}
  eve.views
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console]
            [eve.state :refer [state assoc-in-state!]])
  (:require-macros [dommy.macros :as m]))

; Home
;*******************************************************************************

(defn home [trends]
  (swap-view! (t/home trends))
  (let [trends-list (m/sel1 :#trends)]
    (doseq [trend trends]
      (let [trend-node (t/trend-card trend)]
        (dommy/append! trends-list trend-node)
        (dommy/listen! trend-node :click (fn [& _] (navigate! :trend trend))))))) 
; Trend
;*******************************************************************************

(defn add-datum-to-feed
  [
   ;; {text :text user :user created-at :created-at name :name
   ;;  screen-name :screen-name profile-image-url-https :profile-image-url-https}
   ;; orientation
   datum]
  (dommy/append! (m/sel1 :#feed) (t/datum-card datum)))

(defn trend [trend]
  (swap-view! (t/trend trend))
  ;; NOTE, Wed Oct 16 2013, Francis Wolke
  ;; The rest of the feed updates are handled in `eve.state' via a watcher
  (doseq [datum (:datums @state)]
    (add-datum-to-feed datum)) 
  (dommy/listen! (m/sel1 :#home-button) :click (fn [& _] (navigate! :home))))

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
              (home (take 9 (:trends @state))))))
