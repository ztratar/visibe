(ns user
  (:require [clojure.repl :refer :all]
            [cljs.repl.browser :refer [repl-env]]
            [cemerick.piggieback :as p]
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
