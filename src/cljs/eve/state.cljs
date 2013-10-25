(ns eve.state
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console])
  (:require-macros [dommy.macros :as m]))



(defn add-datum-to-feed
  [
   ;; {text :text user :user created-at :created-at name :name
   ;;  screen-name :screen-name profile-image-url-https :profile-image-url-https}
   ;; orientation
   {type :type :as datum}]
  ;; TODO, Thu Oct 24 2013, Francis Wolke
  ;; this does not belong here
  (case type 
    :instagram
    :vine
    :tweet (if (dommy/html (m/sel1 :#feed))
             (dommy/prepend! (m/sel1 :#feed) (t/datum-card datum))
             (dommy/append! (m/sel1 :#feed) (t/datum-card datum)))))

(def state (atom {:view :home
                  :trends {} 
                  :websocket-connection nil
                  :websocket-functions #{}
                  :last-datum nil
                  :datums []}))





(defn gis
  ;; TODO, Thu Oct 24 2013, Francis Wolke
  ;; When passed a vector, uses it as a path in the `state' map
  ;; When passed a keyword, searches the state map for a matching key.
  "[g]et [i]n [s]tate"
  [path]
  (get-in @state path))

(defn update-in-state!
  ([path f] (swap! state update-in path f))
  ([path f x] (swap! state update-in path f x)))

(defn assoc-in-state! [path v]
  (swap! state assoc-in path v))


