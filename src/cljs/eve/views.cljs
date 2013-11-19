(ns ^{:doc "View logic. Ties together templates, population logic and listeners."}
  eve.views
  (:require [cljs.core.async :as async :refer [<! >! chan put! timeout close!]]
            [cljs.core.match :as match]
            [clojure.set :refer [difference]]
            [dommy.utils :as utils]
            [eve.state :refer [state assoc-in-state! gis]]
            [eve.templates :as t]
            [eve.utils :refer [->slug slug->trend]]
            [eve.ws :refer [wsc]]
            [goog.dom :as gdom]
            [goog.events :as gevents]
            [secretary.core :as secretary]
            [shodan.console :as console]
            [dommy.core :refer [listen! append! prepend! html] :as dommy]
            [goog.History :as ghistory]
            [goog.history.EventType :as history-event]
            [goog.history.Html5History :as history5])
  (:require-macros [secretary.macros :refer [defroute]]
                   [cljs.core.match.macros :refer [match]]
                   [cljs.core.async.macros :refer [go alt!]]
                   [dommy.macros :as m :refer [sel1 sel]]))

(declare history)
(declare navigate!)
(declare swap-view!)

(def dom-helper (goog.dom.DomHelper.))

(defn url->relative-path [s]
  (clojure.string/replace s "http://localhost:9000/" ""))

(defn navigate-callback
  ([callback-fn] (navigate-callback history callback-fn))
  ([hist callback-fn] (gevents/listen hist history-event/NAVIGATE
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

;;; XXX, Sun Nov 17 2013, Francis Wolke
;;; this needs to be part of the `bootstrap!` process
(navigate-callback history history-logic)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Home

(defn home [trends]
  (swap-view! (t/home trends))
  (let [trend-m trends
        trends (loop [acc #{}]
                 (if (= (* 3 (quot (count trends) 3)) (count acc))
                   acc
                   (recur (remove #{"created-at"} (conj acc (rand-nth (keys trend-m)))))))
        token (get-token)]

    ;; Unsubscribe from whatever trend we just left. This does not work when you are attempting to
    ;; do this from the history API.
    (let [trend (slug->trend token)]
      (wsc `(~'unsubscribe! ~trend)))
    
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Trend

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
  (let [l (feed-height :#feed-left)
        r (feed-height :#feed-right)]
    (cond (= l r) :left
          (< l r) :left
          :else :right)))

(defn determine-card
  "Given a datum, hands back it's card"
  [{type :type datum-type :datum-type id :id :as datum}]
  (case (keyword datum-type)
    :instagram-video (t/instagram-video datum)
    :instagram-photo (t/instagram-photo datum)
    :vine (t/vine datum)
    :tweet (t/tweet datum)
    (case type
      "instagram-photo" (t/instagram-video datum)
      "instagram-video" (t/instagram-photo datum)
      "tweet" (t/tweet datum)
      (t/automagic datum))))

(defn add-new-datum! [datum]
  (let [datum-card (determine-card datum)]
    (if (= :left (left-or-right?))
      (prepend! (sel1 :#feed-left) datum-card)
      (prepend! (sel1 :#feed-right) datum-card))))

(defn add-old-datum! [{type :type datum-type :datum-type id :id :as datum}]
  (let [datum-card (determine-card datum)]
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

    (wsc `(~'subscribe! ~trend-string))
    (swap-view! (t/trend trend (trends trend-string)))

    ;; Population and addition of logic
    (dommy/listen! (m/sel1 :#home-button) :click (fn [& _] (navigate! :home)))
    (let [trend-datums (take 15 (reverse (sort-by :created-at (datums-for trend-string))))]
      (if (empty? trend-datums)
        (append! (sel1 :.social-feed) (m/node [:h1 "Preloader."]))
        (do (doseq [d trend-datums]
              (add-new-datum! d))
            (assoc-in-state! [:last-datum] (last trend-datums))
            (wsc `(~'previous-15 ~(last trend-datums))))))))

(defn new-datum-watch!
  "Updates the feed with new datums whenever we recive them"
  [key identity old new]
  (let [current-trend (slug->trend (get-token))
        old-datums (filter #(= current-trend (:trend %)) (:datums old))
        new-datums (filter #(= current-trend (:trend %)) (:datums new))]
    (when (and (= :trend (:view new)))
      (let [to-append (reverse (sort-by :created-at (difference (set new-datums) (set old-datums))))]
        (doseq [datum to-append]
          (add-new-datum! datum))))))

(defn bottom-of-page? []
  (let [document-height (.getDocumentHeight dom-helper)
        document-scroll (aget (.getDocumentScroll dom-helper) "y")
        viewport-height (aget (.getViewportSize dom-helper) "height")]
    (= (- document-height document-scroll) viewport-height)))
                                                
(defn append-old-datums-on-scroll []
  (when (and (= :trend (:view @state))
             (bottom-of-page?)
             (not (sel1 :#no-more-data)))

    (let [last-datum (:last-datum @state)
          sorted-datums (sort-by :created-at (filter #(< (:created-at %) (:created-at last-datum)) (:datums @state)))
          to-append (take 15 (reverse sorted-datums))
          _ (console/log "to-apppend: " to-append)]
      (if (empty? to-append)
        (append! (sel1 :#social-feed) (m/node [:h1#no-more-data "No historical datums"]))
        (do (doseq [datum to-append]
              (add-old-datum! datum))
            (assoc-in-state! [:last-datum] (last to-append))
            (wsc `(~'previous-15 ~(first sorted-datums))))))))

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
