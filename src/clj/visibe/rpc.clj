(ns ^{:doc "Websockets."}
  visibe.rpc)

;; (defn current-trends
;;   "Retuns current trend data by region. Call with no
;;   arguments to retrive a list of avalible regions."
;;   ([] (apply str (cons "please use specify one of more of\n" (interpose "\n" (vals google-mapping)))))
;;   ([& ks] (str "You supplied these keys\n" ks )))

;; (declare rpc-mapping)

;; (defn help
;;   "Returns instructions for use of the websockets api."
;;   []
;;   (str "You can use (doc ...) to get the documentation strings for any function.\n
;; The functions currently avalible to you are:" (keys rpc-mapping)))

;; (defmacro rpc-doc
;;   "Returns a doc-string formatted for a websocket connection."
;;   [v]
;;   `(:doc (meta (var ~v))))

;; (def rpc-mapping '{trends #'visibe.storage/current-trends
;;                    help #'help
;;                    doc #'rpc-doc})

;; (defn call-rpc-fn
;;   ;; XXX, Thu Oct 03 2013, Francis Wolke
;;   ;; This is an obvious security flaw. Look into the EDN reader, clojail, and
;;   ;; slamhound for fixes.
;;   "Safely calls rpc fn."
;;   [{f :fn args :args}]
;;   (let [f (rpc-mapping symbol)]
;;     (cond (and vr (not (empty? args))) (apply vr args)
;;           vr (vr)
;;           :else "Sorry, that function does not exist. Try calling `help'")))

;; (defn rpc-call [s]
;;   (call-rpc-fn (read-string s)))

;; {:fn 'sym :args []}
;; {:caller 'sym :args [] :result ""}


