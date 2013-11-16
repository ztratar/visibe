(ns user
  (:require [clojure.repl :refer :all]
            [cljs.repl.browser :refer [repl-env]]
            [cemerick.piggieback :as p]
            [clj-http.lite.client :as client]
            [cljs.repl :refer [-setup]]
            [clojure.pprint :refer [pprint print-table]]
            [cemerick.pomegranate :refer (add-dependencies)]
            [clojure.reflect :as r]))

(defn c= [n body]
  (= n (count body)))

(defn log [& s]
  (spit (java.io.File. "./log") (apply str s) :append true))

(defn ppn [x]
  (pprint x))

(defn ns-vars
  "Must `use' a ns before using this."
  ([] (ns-vars *ns*))
  ([syms] (pprint (keys (ns-publics syms)))))

(defn cljs-repl []
  (cemerick.piggieback/cljs-repl
   :repl-env (cljs.repl.browser/repl-env :port 8002)))

(defn jmethods [obj]
  (print-table (:members (r/reflect obj))))

(defn add-dep [coordinate]
  (add-dependencies
   :coordinates `[~coordinate]
   :repositories (merge cemerick.pomegranate.aether/maven-central
                        {"clojars" "http://clojars.org/repo"})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Hack

(defn m->ds
  "Accepts a hashmap, returns it's destructuring syntax."
  [m]
  (loop [ks (keys m)
         acc {}]
    (if (empty? ks)
      acc
      (recur (rest ks)
             (let [fk (first ks)
                   v (get m fk)
                   sfk (symbol (apply str (rest (str fk))))]
               (assoc acc (if (map? v) (m->ds v) sfk) fk))))))

(defn paths-to
  "Returns all possible paths to (k)ey within a given (m)ap"
  [k m & context]
  (letfn [(f [[kk mm]] (if context
                         (apply paths-to (cons k (cons mm (cons kk context))))
                         (paths-to k mm kk)))
          (m? [[_ b]] (map? b))]
    (->> (cond (and (m k) (some map? (vals m))) (reduce into [(if context (cons k context) `(~k))
                                                              (map f (filter m? m))])
               (m k) (if context (cons k context) `(~k))
               (some map? (vals m)) (reduce into (map f (filter m? m)))
               :else ())
         (filter #(or (keyword? %) (not (empty? %))))
         (map #(if (coll? %) (reverse %) %)))))


(defn ss
  ;; TODO, Fri Nov 01 2013, Francis Wolke
  ;; Dosn't obey spec. Also, breaks down when I attempted to use it on the `state'. This is because it dosn't
  ;; play nicely with objects as keys (?)
  ;; this could, given some work, be useful for things like data migrations. Just specify the data structure that you want, from what exists
  ;; and use logic programming to figure out how to get there. Possibly.
  "[s]mart [s]elect. 
Given a start map and `seq' of goal-keys, generates a function to convert one to the other (start to goal). EG:
... call ...
;=> (fn [in out] {:key1 (get-in in [:foo :bar]) ...})

 If multiple paths exist to any of the 
goal keys, a hashmap is returned keyed by duplicates where the value is all possible
paths to the duplicate key.

{:duplicate-key [[:path :to :key] [:other :path] ...]}"
  ([start goal-keys]
     (loop [goal-keys goal-keys
            duplicate-keys {}
            out {}]
       (cond (and (empty? goal-keys) (empty? duplicate-keys)) out
             (empty? goal-keys) duplicate-keys
             :else (let [fst (first goal-keys)
                         paths (paths-to fst start)]
                     (if (= 1 (count paths))

                       (recur (rest goal-keys)
                              duplicate-keys 
                              (assoc out fst paths))

                       (recur (rest goal-keys)
                              (assoc duplicate-keys fst paths)
                              out)))))))
