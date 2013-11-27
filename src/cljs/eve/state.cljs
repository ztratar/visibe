(ns eve.state
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console])
  (:require-macros [dommy.macros :as m]))

(def state (atom {:view :home
                  :homepage-layout []
                  :trends {}
                  :mobile false
                  :websocket-connection nil
                  :last-datum nil
                  :datums #{}}))
(defn gis
  "[g]et [i]n [s]tate"
  [path]
  (get-in @state path))

(defn update-in-state!
  ([path f] (swap! state update-in path f))
  ([path f x] (swap! state update-in path f x)))

(defn assoc-in-state! [path v]
  (swap! state assoc-in path v))

(defn ^:export toggle-mobile! []
  (assoc-in-state! [:mobile] (not (gis [:mobile]))))
