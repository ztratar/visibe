(ns ^{:doc "Used to generate urls for popular trending photos. 
Relevent page: http://www.flickr.com/services/api/misc.urls.html"}
  visibe.feeds
  (:use ring.util.codec)
  (:require [clj-http.lite.client :as client]
            [clojure.xml :as xml]
            [visibe.state :refer [gis]])
  (:import java.io.StringReader
           java.net.URL
           java.io.ByteArrayInputStream))

(defn flickr-trend-photo-url
  "Returns a URL associated with a trend"
  ;; TODO, Fri Oct 18 2013, Francis Wolke
  ;; This will pull back images that are too small for our needs. There needs to be 'intelligence' to pick suitable images.
  [trend]
  (let [req-body (:body (client/get (str "http://api.flickr.com/services/rest/?method=flickr.photos.search&api_key="
                                         (gis [:flickr :key]) "&text=" (url-encode trend))))
        url-data (ByteArrayInputStream. (.getBytes req-body "UTF-8"))
        ds (xml/parse url-data)
        ;; NOTE, Fri Oct 18 2013, Francis Wolke
        ;; Photos sorted by most recent (by default)
        {farm-id :farm server-id :server id :id secret :secret} (:attrs (first (:content (first (:content ds)))))
        ]
    (str "http://farm" farm-id ".staticflickr.com/" server-id "/" id "_" secret ".jpg")))


(flickr-trend-photo-url     "Nexus 5")


"http://farm8.staticflickr.com/7408/10347710855_5efe66d507.jpg"
