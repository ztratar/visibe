(ns ^{:doc "Conceptual start of the program."}
  eve.core
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]
            [eve.ws :refer [ws-connect!]]
            [shodan.console :as console]
            [eve.views :refer [navigate! new-datum-watch! bottom-of-page?
                               append-old-datums-on-scroll set-token!]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [eve.state :refer [state assoc-in-state! update-in-state!]]
            [eve.templates :as templates]
            [cljs.core.async :as async :refer [<! >! chan put! timeout close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [dommy.macros :as m]))

(repl/connect "http://localhost:8002/repl")

(defn bootstrap! []
  (set-token! "")
  (ws-connect!)
  (let [ch (chan)]
    (go (loop [v (:trends @state)]
          (>! ch v)
          (when (empty? v)
            (recur (:trends @state)))))
    (go (loop []
          (let [v (<! ch)]
            (if (empty? v)
              (recur)
              (do (navigate! :home)
                  (close! ch)))))))
  (add-watch state :feed new-datum-watch!)
  (set! (-> js/videojs (.-options) (.-flash) (.-swf)) "js/video-js/video-js.swf"))

(def on-load (set! (.-onload js/window) bootstrap!))
(def on-scroll (set! js/window.onscroll append-old-datums-on-scroll))
