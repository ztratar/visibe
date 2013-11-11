(ns ^{:doc "View logic. Ties together templates, population logic and listeners."}
  eve.views
  (:require [dommy.core :refer [listen! append! prepend! html] :as dommy]
            [dommy.utils :as utils]
            [eve.templates :as t]
            [shodan.console :as console]
            [cljs.core.async :as async :refer [<! >! chan put! timeout close!]]
            [eve.state :refer [state assoc-in-state! gis]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]
                   [dommy.macros :as m :refer [sel1]]))

;;; FIXME, Sat Oct 19 2013, Francis Wolke
;;; trends -> topics

(declare swap-view!)
(declare navigate!)

; Home
;*******************************************************************************

(defn home [trends]
  (let [trend-m trends
        trends (loop [acc #{}]
                 (if (= (* 3 (quot (count trends) 3)) (count acc))
                   acc
                   (recur (conj acc (rand-nth (keys trend-m))))))]
    (swap-view! (t/home trends))
    (let [trends-list (m/sel1 :#trends)]
      (doseq [trend trends]
        (let [trend-card (t/trend-card trend)
              trend-card-background (str "url(" (:full (trend-m trend)) ")")]
          (dommy/append! trends-list trend-card)
          (dommy/set-style! (sel1 trend-card :span) :background trend-card-background)
          (dommy/listen! trend-card :click (fn [& _] (navigate! :trend trend))))))))

; Trend
;*******************************************************************************

(defn add-new-datum! [{type :type id :id :as datum} & orientation]
  (let [datum-card (case type
                     :instagram-video (t/instagram-video datum)
                     "instagram-video" (t/instagram-video datum)
                     :instagram-photo (t/instagram-photo datum)
                     "instagram-photo" (t/instagram-photo datum)
                     :vine (t/vine datum)
                     "vine" (t/vine datum)
                     :tweet (t/tweet datum)
                     "tweet" (t/tweet datum)
                     (t/automagic datum))]
    (cond (= "" (sel1 :#feed-left) (sel1 :#feed-right)) (append! (sel1 :#feed-left) datum-card)
          (= "" (sel1 :#feed-left)) (append! (sel1 :#feed-left) datum-card)
          (= "" (sel1 :#feed-right)) (append! (sel1 :#feed-right) datum-card)
          (= :left orientation) (prepend! (sel1 :#feed-left) datum-card)
          (= :right orientation) (prepend! (sel1 :#feed-right) datum-card)
          :else (prepend! (sel1 :#feed-right) datum-card))))

(defn datums-for
  "Returns datums associated with the specified trend"
  [trend]
  (filter (comp (partial = trend) :trend) (gis [:datums])))

(defn trend [trend]
  ;; Swap views
  (let [img-m ((:trends @state) trend)
        thumbnail (:thumb img-m)
        thumbnail (when thumbnail (str "http://localhost:9000/cropped-images/" thumbnail ".png"))]
    (swap-view! (t/trend trend (if thumbnail thumbnail (:full img-m)))))

  ;; Population and addition of logic
  (dommy/listen! (m/sel1 :#home-button) :click (fn [& _] (navigate! :home)))
  (let [trend-datums (datums-for trend)]
    (if (empty? trend-datums)
      ;; TODO, FIXME Sun Nov 10 2013, Francis Wolke Realistially, this should
      ;; never happen (in production), but if it does, we can just display
      ;; another trend (that has some dautms associated with it)
      (append! (sel1 :#feed) (m/node [:h1 "Hate to dissapoint, but there isn't any info on this trend right now."]))
      (loop [d trend-datums
             left? true]
        (when-not (empty? d)
          (if left?
            (add-new-datum! d :left)
            (add-new-datum! d :right))
          (recur (rest d)
                 (not left?)))))))

(defn feed-update! [key identify old new]
  (case (:view @state)
    :trend (let [to-add (take-while (partial not= (first (:datums old)))
                                    (:datums new))]
             ;; XXX, Sun Nov 10 2013, Francis Wolke
             ;; Add-new-datum! should have the logic for nazi stepping. 
             #_(doseq [d (reverse to-add)]
               
               (add-new-datum! d )))
    (console/log "Feed update NoOp")))

; misc
;*******************************************************************************

(defn swap-view! [node]
  ;; TODO, Mon Oct 14 2013, Francis Wolke
  ;; Hide these instead of replacing contents? 
  (dommy/replace-contents! (m/sel1 :#content) node)) 

(defn navigate! [view & args]
  ;; TODO, Tue Oct 15 2013, Francis Wolke
  ;; Buggy - if you don't pass in a trend, it'll throw. Add checks.
  (case view
    :trend (do (apply trend args)
               (assoc-in-state! [:view] :trend))
    :home (do (home (:trends @state))
              (assoc-in-state! [:view] :home))))
