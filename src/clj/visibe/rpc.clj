(ns ^{:doc "RPC logic for websocket connections."}
  visibe.rpc
  (:use user)
  (:require [visibe.feeds.google-trends :refer [google-mapping]]
            [visibe.core :refer [state]]))

;;; TODO, Sat Oct 05 2013, Francis Wolke
;;; Clojure core supplies arglists in metadata. Move it from the comments to
;;; there. Perhaps a macro could do this?

(declare rpc-fns)

;;; FIXME, XXX Sat Oct 05 2013, Francis Wolke
;;; The next three functions are all actually implemented in 'visibe.homeless',
;;; and the fourth has not been implemented at all. The layout is due to some
;;; circular dependency issues I'll fix at a later date.

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

(defn datums-from
  "([time])
Returns the 50 previous datums from the specified time."
  [id]
  "No implementation yet.")

;;; Everything from here on is as it seems, even if it's in the wrong place.

(defn current-trends
  "([] [region])
Call with no arguments to retrive a list of avalible regions.
Call with a region to retrive trend data for that region.
EG: (current-trends :canada)"
  ([] (apply str (cons "please use specify one of more of\n"
                       (interpose "\n" (vals google-mapping)))))
  ([region] 
     #_(apply str (into ["Please note that I'm not dealing with regions right now. The top trends for the united states are:\n"]
                      (interleave (repeat "\n")
                                  (get-in @state [:app :trends]))))
     (str (get-in @state [:app :trends]))))

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
           'doc  {:var #'rpc-doc
                  :doc "([name])\nPrints documentation for a var or special form
given its name"}
           
           'close-stream {:var #'close-stream
                         :doc (doc-str stop-stream)}

           'open-stream  {:var #'open-stream
                           :doc (doc-str start-stream)}

           'toggle-stream-encoding! {:var #'toggle-stream-encoding!
                                           :doc (doc-str toggle-stream-encoding!)}
           
           'datums-from  {:var #'datums-from
                          :doc (doc-str datums-from)}})

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
