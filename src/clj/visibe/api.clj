(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [visibe.state :refer [state]]))

;;; TODO, Tue Oct 08 2013, Francis Wolke

;;; Create a `defn-api-fn' macro that creates a route for the function. It
;;; should be aware of the data encoding specified by the client.

; Boilerplate
;*******************************************************************************

(def ^{:doc "Consumed by X which creates doc functions and `api-routes'."}
  http-fns)

(defn create-channel!
  "Creates a new channel"
  [channel])

(defn destroy-channel!
  "Destroys a channel"
  [channel])


; WebSockets
;*******************************************************************************

;;; It dosn't make any sense to have funtion calls made over websockets. HTTP
;;; Will work for that, and then we can have websockets stream the data. This
;;; will prevent any sort of duality of the API having to decide 'where' it
;;; needs to call the function.

; HTTP
;*******************************************************************************

(defn open-stream!
  "Begins streaming real time data to the client on the specifed trend"
  [channel trend]
  )

(defn close-stream!
  "Stops streaming data on the current trend to the specified channel"
  [channel trend]
  )


(defn toggle-encoding!
  "Toggles between JSON or EDN as encoding for data returned from the API"
  []
  )

(defn current-trends
  "Returns current google trends"
  []
  (get-in @state [:app :trends]))

(defn previous-datums
  "Returns the 50 last (sorted chronologically)"
  [id]
  )

(defn doc
  "Returns documentation associated with a symbol that is part of the API, HTTP
or websockets."
  ;; TODO, Tue Oct 08 2013, Francis Wolke
  ;; Try via HTTP and WS to see which feels more natural
  [sym]
  )


