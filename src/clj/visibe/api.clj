(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [org.httpkit.server :as hk]
            [visibe.state :refer [state assoc-in-state! update-in-state!]]
            [visibe.feeds.google-trends :as [google-mapping]]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.handler :as handler]))

; Boilerplate
;*******************************************************************************

(defn fn-name->route [sym]
  (clojure.string/replace (str sym) "-" "/"))

(defmacro expose [sym]
  `(~'POST ~(str "/api/" (fn-name->route sym))
           {~'body :body}
           (str (let [~'body (when ~'body (read-string (slurp ~'body)))]
                  (if ~'body
                    (apply ~sym ~'body)
                    (~sym))))))

; WebSockets
;*******************************************************************************

;;; Note, Sat Oct 12 2013, Francis Wolke

;;; Make detailed notes about how this websocket scheme works so that it may be
;;; citiqued without others having to read the code. Additionally, this will
;;; allow me to more easily identify it's flaws.

;;; TODO, Sat Oct 12 2013, Francis Wolke
;;; Add tests

(defn destroy-channel!
  "Destroys a channel"
  [channel]
  (update-in-state! [:app :channels] dissoc channel))

(defn add-trend-stream!
  "Adds a new trend stream to a channel."
  [channel trend]
  (update-in-state! [:app :channels channel :trends] conj trend))

(defn remove-trend-stream!
  "Removes trend stream from a channel."
  [channel trend]
  (update-in-state! [:app :channels channel :trends] (partial remove #{trend})))

(defn register-new-channel!
  "Adds new channel and associated context to `state'"
  ;; TODO, Sat Oct 12 2013, Francis Wolke
  ;; I doubt that identifying a client by a websocket connection is a good
  ;; idea. Modify so we use some sort of UUID scheme. Also, ask aound on IRC.
  [channel trend]
  (assoc-in-state! [:app :channels channel]
                   {:trends #{trend}))

(defn open-trend-stream!
  "Registers a new channel with trend if it does not already exist, and adds a
  new trend stream if it does."
  [channel trend]
  (if ((get-in @state [:app :channels]) channel)
    (add-trend-stream! channel trend)
    (register-new-channel! channel trend)))

(defn websocket-handler
  [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [& _] (destroy-channel! channel)))
    (hk/on-receive channel (fn [data] (hk/send! channel (str "ECHO:" data))))))

; HTTP
;*******************************************************************************

(defn current-trends
  "Returns current google trends for specified region"
  []
  (get-in @state [:google :trends]))

(defn regions
  "Returns trending regions"
  []
  (vals (google-mapping)))

(defn previous-50-datums
  "Returns the 50 last (sorted chronologically)"
  [id]
  "previous-50-datums")

(defroutes api-routes
  (expose current-trends)
  (expose regions)
  (expose previous-50-datums))
