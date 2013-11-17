(ns ^{:doc "Conceptual start of the program."}
  eve.core
  (:require [clojure.browser.repl :as repl]
            [cljs-http.client :as http]

            [eve.ws :refer [ws-connect!]]
            [shodan.console :as console]
            [eve.views :refer [feed-update! navigate! new-datum-watch! set-token! bottom-of-page?]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [eve.state :refer [state assoc-in-state! update-in-state!]]
            [eve.templates :as templates]
            [cljs.core.async :as async :refer [<! >! chan put! timeout close!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [dommy.macros :as m]))

;; http://marcopolo.io/2013/10/01/servant-cljs.html

(repl/connect "http://localhost:8002/repl")

(defn bootstrap! []
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
  (set-token! "")
  (set! (-> js/videojs (.-options) (.-flash) (.-swf)) "js/video-js/video-js.swf"))


(def on-load (set! (.-onload js/window) bootstrap!))
;; (def on-scroll (set! js/window.onscroll (fn [& _] (when (and (= :trend (:view @state)) (bottom-of-page?))
;;                                                     (js/alert "append 15 historical datums")))))

