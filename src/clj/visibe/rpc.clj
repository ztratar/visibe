(ns ^{:doc "RPC logic for websocket connections."}
  visibe.rpc
  (:use user)
  (:require [cheshire.core :refer [encode]]
            [visibe.schemas :refer [random-str]]
            #_[visibe.core :refer [state]]))

;;; TODO, Sat Oct 05 2013, Francis Wolke
;;; Clojure core supplies arglists in metadata. Move it from the comments to
;;; there. Perhaps a macro could do this?

(declare rpc-fns)

;;; FIXME, XXX Sat Oct 05 2013, Francis Wolke
;;; The next three functions are all actually implemented in 'visibe.homeless',
;;; and the fourth has not been implemented at all. The layout is due to some
;;; circular dependency issues I'll fix at a later date.

(def google-mapping
  ;; NOTE, Mon Sep 30 2013, Francis Wolke
  ;; I have no idea why they're ranked like this (and have missing keys). The
  ;; second version of the should be able to figure out countries itself.
  {"1" :united-states
   "3" :india
   "4" :japan
   "5" :singapore
   "6" :israel
   "8" :australia
   "9" :united-kingdom
   "10" :hong-kong
   "12" :taiwan
   "13" :canada
   "14" :russia
   "15" :germany})

(defn toggle-stream-encoding!
  "([])
Toggles between sending JSON or EDN over the stream"
  [trend]
  "See 'visibe.homeless'")

(defn open-stream
  "([trend])
Starts streaming datums related to the given trend"
  [trend]
  "See 'visibe.homeless'")

(defn close-stream
  "([trend])
Stop streaming datums related to the given trend"
  [trend]
  "See 'visibe.homeless'")

(defn previous-datums
  "([datum])
Accepts a JSON representation of a datum and returns the previous chunk."
  [datum]
  "Has not yet been implemented.")

;;; Everything from here on is as it seems, even if it's in the wrong place.

(defn current-trends
  "([] [region])
Call with no arguments to retrive a list of avalible regions.
Call with a region to retrive trend data for that region.
EG: (current-trends :canada)

XXX: Currenty returns trends for the united states no matter which region is
 passed in"
  ([] (apply str (cons "please use specify one of more of\n"
                       (interpose "\n" (vals google-mapping)))))
  ([region] (encode (take 10 (iterate (fn [_] (random-str 20)) (random-str 20))))))

(defn help
  "([])
Returns instructions for use of the websockets api."
  []
  (apply str (into ["-------------------------\n" "You can use (doc name-of-function) to get the documentation strings for any function.\n
The functions currently avalible to you are:\n"] (interleave (repeat "\n") (keys
                                                                            rpc-fns)))))

(defmacro doc-str
  [sym]
  `(:doc (meta (var ~sym))))

(defn generate-docs [f doc-string]
  ;; TODO, Fri Oct 04 2013, Francis Wolke
  ;; Docs are not currently created dynamically.
  
  ;; This should fix the problem
  ;; (defmacro foo [fm]
  ;; `(str ~(second fm)))
  
  (str "-------------------------\n" f "\n" doc-string))

(def ^{:doc "Functions avalible to client"}
  rpc-fns {'trends {:var #'current-trends
                    :doc (doc-str current-trends)}
           
           'help {:var #'help
                  :doc (doc-str help)}
           ;; NOTE, Fri Oct 04 2013, Francis Wolke
           ;; Doc is a special case. Eww.
           'doc  {:var #'generate-docs
                  :doc "([name])\nPrints documentation for a var or special form
given its name"}
           
           'close-stream {:var #'close-stream
                          :doc (doc-str close-stream)}

           'open-stream  {:var #'open-stream
                          :doc (doc-str open-stream)}

           'toggle-stream-encoding! {:var #'toggle-stream-encoding!
                                     :doc (doc-str toggle-stream-encoding!)}
           
           'previous-datums  {:var #'previous-datums
                              :doc (doc-str previous-datums)}})

(defn rpc-call
  
  ;; TODO, Thu Oct 03 2013, Francis Wolke
  
  ;; This is an obvious security flaw. Look into the EDN reader, clojail, and
  ;; slamhound for fixes.

  ;; I really don't want to lose the caller information, and calling in LISP
  ;; form and passing back a map feels wrong. And though moving away from sexps
  ;; is wrong (because we lose expsessivity) I think that using maps for the
  ;; time being is an acceptable solution.

  ;; {:fn 'sym :args []}
  ;; {:caller 'sym :args [] :result ""}

  ;; We need to evaluate all of this in a 'safe' namespace.

  ;; add `finally' block?
  
  [s]
  (let [fm (read-string s)
        f (first fm)
        r (rest fm)
        fn-info (get rpc-fns f)]
    
    ;; FIXME, Fri Oct 04 2013, Francis Wolke
    ;; Documentation will grab the first thing off the list and ignore the
    ;; rest of the elements. We should throw an exception.

    ;; `(doc fn-that-does-not-exist)` will return a partial docstring instead of
    ;; an error.
    
    (try (cond (= f 'doc) (if fn-info
                            (generate-docs (first r) (:doc (rpc-fns (first r))))
                            "That function does not exist.")
               f (apply (:var fn-info) r)
               ;; XXX, Fri Oct 04 2013, Francis Wolke
               ;; This does not always work.
               :else (str s "\n\nIs not supported. Try calling `(help)`"))
         (catch Exception e (.getMessage e)))))
