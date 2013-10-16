(ns ^{:doc "View logic. Ties together templates and population logic."}
  eve.views
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console]
            [eve.state :refer [state assoc-in-state!]])
  (:require-macros [dommy.macros :as m]))

; misc
;*******************************************************************************

(defn swap-view! [node]
  ;; TODO, Mon Oct 14 2013, Francis Wolke
  ;; Hide these instead of replacing contents? 
  (dommy/replace-contents! (m/sel1 :#content) node)) 

; Home
;*******************************************************************************

(defn home [trends]
  (swap-view! (t/home trends))
  (let [trends-list (m/sel1 :#trends)]
    (doseq [trend trends]
      (let [trend-node (t/trend-card trend)]
        (dommy/append! trends-list trend-node)
        (dommy/listen! trend-node :click
                       (fn [& _] (navigate! :trend trend)))))))

; Trend
;*******************************************************************************

(defn add-datum-to-feed
  [{text :text user :user created-at :created-at name :name
    screen-name :screen-name profile-image-url-https :profile-image-url-https}]
  (dommy/prepend! (m/sel1 :#feed)
                  (m/node [:li.feed-datum
                           [:div.tweet [:ul
                                        [:li [:img {:src "zach_profile.png"
                                                    :width "100px" :height "100px"}]]
                                        [:li "text" text]
                                        [:li "user" user]]]])))

(defn trend [trend]
  (swap-view! (t/trend trend))
  ;; NOTE, Wed Oct 16 2013, Francis Wolke
  ;; The rest of the feed updates are handled in `eve.state' via a watcher
  (doseq [datum (:datums @state)]
    (add-datum-to-feed datum))
  (dommy/listen! (m/sel1 :#home-button) :click  (fn [& _] (navigate! :home))))

(defn navigate! [view & args]
  ;; TODO, Tue Oct 15 2013, Francis Wolke
  ;; Needs to update `state :view'
  ;; This has a bug, if you don't pass anything to trend, it'll work, but without a trend,
  ;; It should throw, unless the issue is me not catching it??
  (case view
    :trend (apply trend args)
    :home (home (:trends @state))))
