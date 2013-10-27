(ns user
  (:require [clojure.repl :refer :all]
            [cljs.repl.browser :refer [repl-env]]
            [cemerick.piggieback :as p]
            [cljs.repl :refer [-setup]]
            [clojure.pprint :refer [pprint print-table]]
            [cemerick.pomegranate :refer (add-dependencies)]
            [clojure.reflect :as r]))

;; (defn ss
;;   "[S]mart [s]elect. Given a start map and set of goal-keys, generates a function
;; to convert one to the other (start to goal). EG:
;; ... call ...
;; ;=> (defn foo [in out] {:key1 (get-in in [:foo :bar]) ...})
    

;;  If multiple paths exist to any of the 
;; goal keys, a hashmap is returned {:duplicate-key [[:path :to :key] [:other :path] ...]} keyed by 
;; duplicates with the "
;;   ([start goal-keys]
;;      (loop [goal-keys]))
;;   ([start goal-keys path-map]))

(defn m->ds
  "Accepts a hashmap, which is then converted into its destructuring syntax."
  [m]
  (loop [ks (keys m)
         acc {}]
    (if (empty? ks)
      acc
      (recur (rest ks)
             (let [fk (first ks)
                   v (get m fk)
                   sfk (symbol (apply str (rest (str fk))))]
               (assoc acc (if (map? v) (map->ds v) sfk) fk))))))

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
