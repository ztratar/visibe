(ns ^{:doc "View logic. Ties together templates, population logic and listeners."}
  eve.views
  (:require [dommy.core :refer [listen! append! prepend! html] :as dommy]
            [dommy.utils :as utils]
            [eve.utils :refer [->slug]]
            [eve.templates :as t]
            [cljs.core.match :as match]
            [shodan.console :as console]
            [eve.core :refer [wsc]]
            [secretary.core :as secretary]
            [cljs.core.async :as async :refer [<! >! chan put! timeout close!]]
            [eve.state :refer [state assoc-in-state! gis]]
            [goog.events :as gevents]
            [goog.History :as ghistory]
            [goog.history.EventType :as history-event]
            [goog.history.Html5History :as history5])
  (:require-macros [secretary.macros :refer [defroute]]
                   [cljs.core.match.macros :refer [match]]
                   [cljs.core.async.macros :refer [go alt!]]
                   [dommy.macros :as m :refer [sel1 sel]]))

(declare swap-view!)
(declare navigate!)
(declare history)

(defn url->relative-path [s]
  (clojure.string/replace s "http://localhost:9000/" ""))          

(defn slug->trend [slug]
  (some (fn [e] (when (= (->slug e) slug) e)) (keys (:trends @state))))

; HTML5 History
;*******************************************************************************

(defn navigate-callback
  ([callback-fn]
     (navigate-callback history callback-fn))
  ([hist callback-fn]
     (gevents/listen hist history-event/NAVIGATE
                     (fn [e]
                       (callback-fn {:token (keyword (.-token e))
                                     :type (.-type e)
                                     :navigation? (.-isNavigation e)})))))

(defn init-history
  []
  (let [history (if (history5/isSupported)
                  (goog.history.Html5History.)
                  (goog.History.))]
    (.setEnabled history true)
    (gevents/unlisten (.-window_ history)
                      (.-POPSTATE gevents/EventType) ; This is a patch-hack to ignore double events
                      (.-onHistoryEvent_ history), false, history)
    history))

(defn history-logic [{type :type token :token navigation? :navigation? :as m}]
  (let [token (name token)]
    (cond (or (= "#" token) (= "" token)) (navigate! :home)
          (some #{token} (map ->slug (keys (:trends @state)))) (navigate! :trend (name token))
          :else (console/error "attempted to `navigate!' to " (str token)))))

(def history (init-history))

(defn get-token
  ([] (get-token history))
  ([hist] (.getToken hist)))

(defn set-token!
  ([tok] (set-token! history tok))
  ([hist tok] (.setToken hist tok)))

(defn replace-token! [hist tok] (.replaceToken hist tok))

;;; TODO, Fri Nov 15 2013, Francis Wolke
;;; This should instead be placed into `bootstrap!'
(navigate-callback history history-logic)

; Home
;*******************************************************************************

(defn home [trends]
  (let [trend-m trends
        trends (loop [acc #{}]
                 (if (= (* 3 (quot (count trends) 3)) (count acc))
                   acc
                   (recur (conj acc (rand-nth (keys trend-m))))))
        token (get-token)]

    ;; Unsubscribe from whatever trend we just left. This does not work when you are attempting to
    ;; do this from the history API.
    (let [trend (slug->trend token)]
      ;; (console/log "trying to unsubscribe from" trend)
      (wsc `(~'unsubscribe! ~trend)))
    
    (swap-view! (t/home trends))
    ;; Population
    (let [trends-list (m/sel1 :#trends)]
      (doseq [trend trends]
        (let [trend-card (t/trend-card trend)
              trend-card-background (str "url(" (trend-m trend) ")")]
          (dommy/append! trends-list trend-card)
          (dommy/set-style! (sel1 trend-card :span) :background trend-card-background)
          (dommy/listen! (sel1 trend-card :a)
                         :click (fn [e]
                                  (.preventDefault e)
                                  (let [new-path (url->relative-path (.-href (sel1 trend-card :a)))]
                                    (set-token! new-path)
                                    (navigate! :trend new-path)))))))))

; trend
;*******************************************************************************

(defn feed-height
  [feed]
  (let [e (.-childNodes (sel1 feed))
        r (range (.-length e))
        f (fn [g] (cond (.contains (.-classList g) "instagram") 2
                        (.contains (.-classList  g) "tweet") 1
                        :else (console/error "This social activity did not have one #{tweet, instagram-photo, instagram-video} as a class:" g)))]
    (if (empty? r)
      0
      (reduce + (map (comp f (partial aget e)) r)))))

(defn left-or-right? []
  (letfn []
    (let [l (feed-height :#feed-left)
          r (feed-height :#feed-right)]
      (cond (= l r) :left
            (< l r) :left
            :else :right))))

(defn add-new-datum! [{type :type datum-type :datum-type id :id :as datum}]
  (let [datum-card (case (keyword datum-type)
                     :instagram-video (t/instagram-video datum)
                     :instagram-photo (t/instagram-photo datum)
                     :vine (t/vine datum)
                     :tweet (t/tweet datum)
                     (case type
                       "instagram-photo" (t/instagram-video datum)
                       "instagram-video" (t/instagram-photo datum)
                       "tweet" (t/tweet datum)
                       (t/automagic datum)))]
    (if (= :left (left-or-right?))
      (append! (sel1 :#feed-left) datum-card)
      (append! (sel1 :#feed-right) datum-card)))) 

(defn datums-for
  "Returns datums associated with the specified trend"
  [trend]
  (filter (comp (partial = trend) :trend) (gis [:datums])))

(defn trend [trend]
  ;; Swap views
  (let [trends (:trends @state)
        trend-string (some (fn [e] (when (= (->slug e) trend) e)) (keys trends))]

    ;; (console/log "subscribing to" trend-string)
    (wsc `(~'subscribe! ~trend-string))
    (swap-view! (t/trend trend (trends trend-string)))
    
    ;; Population and addition of logic
    (dommy/listen! (m/sel1 :#home-button) :click (fn [& _] (navigate! :home)))
    (let [trend-datums (datums-for trend-string)]
      (if (empty? trend-datums)
        (append! (sel1 :#feed) (m/node [:h1 "Preloader."]))
        (doseq [d trend-datums]
          (add-new-datum! d))))))

; misc
;*******************************************************************************

(defn swap-view! [node]
  ;; TODO, Mon Oct 14 2013, Francis Wolke
  ;; Hide these instead of replacing contents? 
  (dommy/replace-contents! (m/sel1 :#content) node)) 

(defn navigate! [view & args]
  ;; TODO, Tue Oct 15 2013, Francis Wolke Buggy - if you don't pass in a trend,
  ;; it'll throw. Clojure has some sort of contracts facility...
  (case view
    :trend (do (apply trend args)
               (assoc-in-state! [:view] :trend))
    :home (do (home (:trends @state))
              (assoc-in-state! [:view] :home))))
