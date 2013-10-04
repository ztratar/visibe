(ns ^{:doc "RPC logic for websocket connections."}
  visibe.rpc
  (:use user )
  (:require [visibe.feeds.google-trends :refer [google-mapping]]))

(defn current-trends
  "Retuns current trend data by region. Call with no
  arguments to retrive a list of avalible regions."
  ([] (apply str (cons "please use specify one of more of\n"
                       (interpose "\n" (vals google-mapping)))))
  ([& ks] (str "You supplied these keys\n" ks )))

(declare rpc-mapping)

(defn help
  "Returns instructions for use of the websockets api."
  []
  (apply str (into ["You can use (doc FUNCTION) to get the documentation strings for any function.\n
The functions currently avalible to you are:\n"] (interleave (repeat "\n") (keys rpc-mapping)))))

(def ^{:doc "Functions avalible ap"}
  rpc-fns {'trends #'current-trends
           'help #'help
           'doc #'rpc-doc})

(defmacro rpc-doc
  "Returns a doc-string formatted for a websocket connection."
  ;; FIXME, Fri Oct 04 2013, Francis Wolke
  ;; Dosn't work. Issue with quoting.
  [sym]
  (when-let [f (get rpc-fns sym)]
    `(:doc (meta ~f))))

(defn rpc-call
  ;; TODO, Thu Oct 03 2013, Francis Wolke
  ;; This is an obvious security flaw. Look into the EDN reader, clojail, and
  ;; slamhound for fixes.

  ;; I really don't want to lose the caller information, but calling in LISP
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
        r (rest fm)]
    (try (if (= f 'doc) 
           "doc does not currently work."
           (apply (get rpc-fns f) r))
         (catch Exception e (.getMessage e)))))
