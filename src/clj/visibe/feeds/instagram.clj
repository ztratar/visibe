(ns visibe.feeds.instagram
  "Boilerplate for gathering trend data from instagram"
  (:use instagram.oauth
        ring.util.codec
        instagram.callbacks
        instagram.callbacks.handlers
        instagram.api.endpoint)
  (:require [visibe.state :refer [assoc-in-state! gis]]
            [org.httpkit.server :as hk]
            [visibe.homeless :refer [sleep]]
            [visibe.api :refer [ds->ws-message]]
            [visibe.feeds.storage :refer [append-datums]]
            [visibe.homeless :refer [rfc822-str->long]]
            [clj-time.coerce :refer [from-long]])
  (:import instagram.callbacks.protocols.SyncSingleCallback))

(defn generate-oauth-creds! []
  (assoc-in-state! [:instagram :creds]
                   (make-oauth-creds (gis [:instagram :client-id])
                                     (gis [:instagram :client-secret])
                                     (gis [:instagram :redirect-url]))))

(defn- trend->tag [trend]
  ;; XXX, Thu Oct 24 2013, Francis Wolke
  ;; Currently discarding all but one of the returned tags.
  (let [trend (clojure.string/replace trend " " "")]
    (:name (first (:data (:body (search-tags :oauth (gis [:instagram :creds]) :params {:q (url-encode trend)})))))))

(defn instagram-media
  "Accepts a trend, converts it to a tag name, searches instagram and returns
   relevent media."
  [trend]
  ;; FIXME, Thu Oct 24 2013, Francis Wolke
  ;; Currently we are not grabbing all of the data that we could be. Make use of
  ;; the pagination feature.
  (:data (:body (get-tagged-medias :oauth (gis [:instagram :creds])
                                   :params {:tag_name (trend->tag trend)}))))
