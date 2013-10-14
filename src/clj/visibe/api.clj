(ns ^{:doc "Websocket and HTTP API."}
  visibe.api
  (:require [org.httpkit.server :as hk]
            [visibe.state :refer [state assoc-in-state! update-in-state!]]
            [visibe.feeds.google-trends :refer [google-mapping]]
            [visibe.feeds.storage :refer [previous-50-datums]]
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
;;; Add client-side tests for API.

(defn destroy-channel!
  "Destroys a channel"
  [channel]
  (update-in-state! [:app :channels] dissoc channel))

(defn register-new-channel!
  "Adds new channel and associated context to `state'"
  ;; TODO, Sat Oct 12 2013, Francis Wolke
  ;; I doubt that identifying a client by a websocket connection is a good
  ;; idea. Modify so we use some sort of UUID scheme. Also, ask aound on IRC.
  [channel]
  (assoc-in-state! [:app :channels channel] {:trends #{}}))

(defn ^{:api :websocket :doc "Adds a new trend stream to a channel."}
  open-trend-stream!
  [channel trend]
  ;; The issue is with channel equality, try from server side and add tests.
  (update-in-state! [:app :channels channel :trends] (fn [& args] (set (apply conj args))) trend))

(defn ^{:api :websocket :doc "Removes trend stream from a channel."}
  close-trend-stream!
  [channel trend]
  (update-in-state! [:app :channels channel :trends]
                    (fn [st] (set (remove #{trend} st)))))

(defn ws-api-call [channel data]
  (when-not (get-in @state [:app :channels channel])
    (register-new-channel! channel))
  (let [ds (read-string data)
        fst (first ds)]
    ;; TODO, Sun Oct 13 2013, Francis Wolke
    ;; Don't pass back all the data about the atom when we're in production
    (cond (= fst 'open-trend-stream!) (open-trend-stream! channel (second ds))
          (= fst 'close-trend-stream!) (close-trend-stream! channel (second ds))
          :else "Not a valid funtion. `help' and `doc' are not yet implemented.")))

(defn websocket-handler
  [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [& _] (destroy-channel! channel)))
    (hk/on-receive channel (fn [data] (hk/send! channel (str (ws-api-call channel data)))))))

; HTTP
;*******************************************************************************

(defn current-trends
  "Returns current google trends for specified region"
  []
  (get-in @state [:google :trends]))

(defn regions
  "Returns trending regions"
  []
  (vals google-mapping))

(defroutes api-routes
  (expose current-trends)
  (expose regions)
  (expose previous-50-datums))
