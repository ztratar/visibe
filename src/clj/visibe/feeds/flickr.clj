(ns ^{:doc "Used to generate urls for popular trending photos. 
Relevent page: http://www.flickr.com/services/api/misc.urls.html"}
  visibe.feeds.flickr
  (:use ring.util.codec)
  (:require [clj-http.lite.client :as client]
            [clojure.xml :as xml]
            [visibe.state :refer [gis]])
  (:import java.io.StringReader
           java.io.File
           java.net.URL
           java.io.ByteArrayInputStream
           javax.imageio.ImageIO))

(defn buffered-images-equal?
  "Checks for equality with 'PROJECT_ROOT/resources/public/flickr-not-found.jpg'"
  [image-1 image-2]
  (let [i1-width (.getWidth image-1)
        i1-height (.getHeight image-1)

        i2-width (.getWidth image-2)
        i2-height  (.getHeight image-2)]

    (and (= i1-width i2-width)
         (= i1-height i2-height))))

(defn too-small? [img]
  (or (>= 300 (.getWidth img))
      (>= 180 (.getHeight img))))

(defn valid-img?
  "Checks for byte equality against 'PROJECT_ROOT/resources/public/flickr-image-not-found.jpg'"
  [[url img]]
  (when-not (or (buffered-images-equal? img (ImageIO/read (File. "./resources/public/flickr-image-not-found.jpg")))
                (too-small? img))
    url))

(defn trend->photo-url
  [trend]
  (let [req-body (:body (client/get (str "http://api.flickr.com/services/rest/?method=flickr.photos.search&api_key="
                                         (gis [:flickr :key]) "&text=" (url-encode trend))))
        url-data (ByteArrayInputStream. (.getBytes req-body "UTF-8"))
        ds (xml/parse url-data)
        ;; NOTE, Fri Oct 18 2013, Francis Wolke
        ;; Photos sorted by most recent (by default)
        raw-photo-data (:content (first (:content ds)))
        photos-essentials (map #(select-keys (:attrs %) [:farm :server :id :secret]) raw-photo-data)
        photo-urls (map (fn [{farm-id :farm server-id :server id :id secret :secret}]
                          (str "http://farm" farm-id ".staticflickr.com/" server-id "/" id "_" secret ".jpg"))
                        photos-essentials)
        imgs-and-urls (map (fn [url] [url (ImageIO/read (URL. url))]) photo-urls)
        valid-and-sorted (sort-by (fn [[_ img]] (+ (.getWidth img) (.getHeight img)))
                                  (filter valid-img? imgs-and-urls))]
    (ffirst valid-and-sorted)))
