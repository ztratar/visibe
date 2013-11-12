(ns eve.state
  (:require [dommy.core :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console])
  (:require-macros [dommy.macros :as m]))

(def state (atom {:view :home
                  :trends {} 
                  :websocket-connection nil
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
