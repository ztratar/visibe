(ns eve.views
  "View logic. Ties together templates, population logic and listeners."
  (:require [cljs.core.async :as async :refer [<! >! chan put! timeout close!]]
            [cljs.core.match :as match]
            [clojure.set :refer [difference]]
            [dommy.utils :as utils]
            [eve.state :refer [state assoc-in-state! gis toggle-mobile!]]
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

(def root-url (str js/window.location.protocol "//" js/window.location.host "/"))

(defn url->relative-path [s]
  (clojure.string/replace s root-url ""))

(defn datums-for
  "Returns (unsorted) datums associated with the specified trend"
  ([trend] (filter (comp (partial = trend) :trend) (gis [:datums])))
  ([trend n] (take n (datums-for trend))))

(defn datum-count [] (count (:datums @state)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; HTML5 History

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

(defn current-trend [] (slug->trend (get-token)))

;;; XXX, Sun Nov 17 2013, Francis Wolke
;;; this needs to be part of the `bootstrap!` process
(navigate-callback history history-logic)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Home

(defn calculate-homescreen-layout! []
  (let [trends (keys (gis [:trends]))
        new-layout (loop [acc #{}]
                     (if (= (* 3 (quot (count trends) 3)) (count acc))
                       (vec acc)
                       (recur (conj acc (rand-nth trends)))))]
    (assoc-in-state! [:homescreen-layout] new-layout)))

(defn home [trend-m]
  (swap-view! (t/home))
  (dommy/remove-class! (sel1 :body) :topic-page)

  (when (empty? (gis [:homescreen-layout]))
    (calculate-homescreen-layout!))

  ;; Unsubscribe from whatever trend we just left. This does not work when you are attempting to
  ;; do this from the history API.
  (let [trend (slug->trend (get-token))]
    (wsc `(~'unsubscribe! ~trend)))
  
  ;; Population
  (let [trends-list (m/sel1 :#trends)]
    (doseq [trend (gis [:homescreen-layout])]
      (let [trend-card (t/trend-card trend)
            trend-card-background (str "url(" (trend-m trend) ")")]
        (dommy/append! trends-list trend-card)
        (dommy/set-style! (sel1 trend-card :span) :background trend-card-background)
        (dommy/listen! (sel1 trend-card :a)
                       :click (fn [e]
                                (.preventDefault e)
                                (let [new-path (url->relative-path (.-href (sel1 trend-card :a)))]
                                  (set-token! new-path)
                                  (navigate! :trend new-path))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Trend

(defn feed-height
  [feed]
  (let [e (.-childNodes (sel1 feed))
        r (range (.-length e))
        f (fn [g] (cond (.contains (.-classList g) "instagram")   2
                        (.contains (.-classList g) "tweet-photo") 2
                        (.contains (.-classList g) "tweet")       1
                        :else (console/error "This social activity did not have one #{tweet, instagram-photo, instagram-video} as a class:" g)))]
    (if (empty? r)
      0
      (reduce + (map (comp f (partial aget e)) r)))))

(defn left-or-right? []
  (let [l (feed-height :#feed-left)
        r (feed-height :#feed-right)]
    (cond (gis [:mobile]) :right
          (= l r) :left
          (< l r) :left
          :else :right)))

(defn determine-card
  "Given a datum, hands back it's card"
  [{type :type datum-type :datum-type id :id :as datum}]
  (case datum-type
    "instagram-video" (t/instagram-video datum)
    "instagram-photo" (t/instagram-photo datum)
    "tweet-photo"     nil ;; (t/tweet-photo datum) 
    "tweet"           (t/tweet datum)
    "vine"            (t/vine datum)
    (t/automagic datum)))

(defn add-new-datum! [datum]
  (when (sel1 :#preloader) (dommy/remove! (sel1 :#preloader)))
  (let [datum-card (determine-card datum)]
    (when datum-card
      (if (= :left (left-or-right?))
        (prepend! (sel1 :#feed-left) datum-card)
        (prepend! (sel1 :#feed-right) datum-card)))))

(defn add-old-datum! [{type :type id :id :as datum}]
  (let [datum-card (determine-card datum)]
    (if (= :left (left-or-right?))
      (append! (sel1 :#feed-left) datum-card)
      (append! (sel1 :#feed-right) datum-card))))

(defn trend [trend]
  (let [trends (:trends @state)
        trend (slug->trend trend)
        elder-datum (first (sort-by :created-at (datums-for trend)))]
    (if (mobile-size?) 
      (assoc-in-state! [:mobile] true)
      (assoc-in-state! [:mobile] false))
    (dommy/add-class! (sel1 :body) :topic-page)
    (swap-view! (t/trend trend (trends trend)))
    (assoc-in-state! [:last-datum] elder-datum)

    ;; WS calls
    (wsc `(~'subscribe! ~trend))
    (wsc `(~'previous-15 ~elder-datum))
    
    ;; Population and addition of logic
    (dommy/listen! (m/sel1 :#home-button) :click (fn [& _] (navigate! :home)))
    (let [trend-datums (take 15 (sort-by :created-at (datums-for trend)))]
      (if (empty? trend-datums)
        (append! (sel1 :.social-feed) (m/node [:h1#preloader "Preloader."]))
        (doseq [d trend-datums]
          (add-new-datum! d))))))

(defn new-datum-watch!
  "Updates the feed with new datums whenever we recive them"
  [key identity old new]
  (when (= :trend (:view new))
    (let [current-trend (slug->trend (get-token))
          f (partial filter #(= current-trend (:trend %)))
          old-datums (f (:datums old))
          new-datums (f (:datums new))]
      (doseq [datum (reverse (sort-by :created-at (difference (set new-datums) (set old-datums))))]
        (add-new-datum! datum)))))

(defn bottom-of-page?
  "Are we close enough to the bottom to load more?"
  []
  ;; TODO, Tue Nov 26 2013, Francis Wolke
  ;; Do I have to instantiate `dom-helper' every time?
  (let [document-height (.getDocumentHeight dom-helper)
        document-scroll (aget (.getDocumentScroll dom-helper) "y")
        viewport-height (aget (.getViewportSize dom-helper) "height")]
    ;; 600 is the offset
    (>= (+ 600 viewport-height document-scroll) document-height)))

(defn append-old-datums-on-scroll
  "Determines when to place historical datums"
  []
  (when (and (= :trend (:view @state)) (bottom-of-page?))
    
    (let [last-datum (:last-datum @state)
          trend-datums (remove (comp not :created-at) (sort-by :created-at (datums-for (slug->trend (get-token)))))
          older-datums (take-while (partial not= last-datum) trend-datums)
          to-append (take 15 (reverse older-datums))]

      (cond (and (empty? to-append) (not (sel1 :#no-more-data))) (append! (sel1 :.social-feed)
                                                                          (m/node [:div.end-of-data [:h1#no-more-data "No historical datums"]]))
            (empty? to-append) (console/log "There are no more datums")
            :else (do (assoc-in-state! [:last-datum] (last to-append))
                      (wsc `(~'previous-15 ~(last to-append)))
                      (doseq [datum to-append]
                        (add-old-datum! datum)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Reactive layout

(defn redisplay-feed!
  ;; TODO, Mon Dec 09 2013, Francis Wolke
  ;; Scroll user back to the point where they were before
  "Grabs the current datums - and redisplays them according to the layout 
   specified by `(gis :mobile)'"
  []
  (console/log "redisplay-feed")
  (doseq [d (sel :.social-activity)] (dommy/remove! d))
  (let [to-add (take-while (partial not= (gis :last-datum))
                           (sort-by :created-at (datums-for (current-trend))))]
    (doseq [datum to-add]
      (add-old-datum! datum))))

(defn mobile-size? []
  (<= (aget (.getViewportSize dom-helper) "width") 768))

(defn reactive-layout-logic! []
  (match [(mobile-size?) (gis [:mobile])]
         [false false]   nil
         [true true]     nil
         [true false]    (do (toggle-mobile!) (redisplay-feed!))
         [false true]    (do (toggle-mobile!) (redisplay-feed!))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Boilerplate

(defn swap-view! [node]
  ;; TODO, Mon Oct 14 2013, Francis Wolke
  ;; Hide these instead of replacing contents? 
  (dommy/replace-contents! (m/sel1 :#content) node)) 

(defn navigate! [view & args]
  ;; TODO, Tue Oct 15 2013, Francis Wolke Buggy - if you don't pass in a trend,
  ;; it'll throw. Clojure has some sort of contracts facility...
  (case view
    :trend (do (apply trend args) (assoc-in-state! [:view] :trend))
    :home (do (home (:trends @state)) (assoc-in-state! [:view] :home))))

