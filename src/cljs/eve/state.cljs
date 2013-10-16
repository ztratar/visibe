(ns eve.state
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils])
  (:require-macros [dommy.macros :as m]))

(defn add-new-datum-to-feed [{text :text user :user created-at :created-at name :name
                              screen-name :screen-name profile-image-url-https :profile-image-url-https}]
  (dommy/prepend! (m/sel1 :#feed)
                  (m/node [:li.feed-datum [:div.tweet [:ul
                                                       [:li [:img {:src "zach_profile.png" :width "100px" :height "100px"}]]
                                                       [:li "text" text]
                                                       [:li "user" user]
                                                       [:li ]]]])))

(def state (atom {:view :home
                  :trends []
                  :websocket-connection nil
                  :websocket-functions #{}
                  :datums []}))

(defn feed-update [key identify old new]
  (case (:view @state)
    :trend (doseq [datum (:datums @state)]
             (add-new-datum-to-feed datum))
    ;; TODO, Wed Oct 16 2013, Francis Wolke
    ;; Instead of a NoOp, remove it? Does this offer anything?
    (.log js/console "Feed update NoOp")))

(defn update-in-state!
  ([path f] (swap! state update-in path f))
  ([path f x] (swap! state update-in path f x)))

(defn assoc-in-state! [path v]
  (swap! state assoc-in path v))

(add-watch state :feed feed-update)

;; (defn add-new-datum-to-feed [{text :text user :user created-at :created-at name :name
;;                               screen-name :screen-name profile-image-url-https :profile-image-url-https}]
;;   (dommy/prepend! (m/sel1 :#feed)
;;                   (m/node [:li.feed-datum [:div.tweet [:ul
;;                                                        [:li [:img {:src "zach_profile.png" :width "100px" :height "100px"}]]
;;                                                        [:li "text" text]
;;                                                        [:li "user" user]
;;                                                        [:li ]]]])))
