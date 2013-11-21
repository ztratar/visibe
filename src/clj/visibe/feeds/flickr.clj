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

(defn valid-img?
  "Checks for size equality against 'PROJECT_ROOT/resources/public/flickr-not-found.jpg'"
  [[w h]]
  (not (or (or (> 300 w) (> 180 h)) (= [w h] [400 247]))))

(defn trend->photo-url
  [trend]
  (let [req-body             (:body (client/get (str "http://api.flickr.com/services/rest/?method=flickr.photos.search&api_key="
                                                     (gis [:flickr :key]) "&text=" (url-encode trend))))
        url-data             (ByteArrayInputStream. (.getBytes req-body "UTF-8"))
        ds                   (xml/parse url-data)
        ;; NOTE, Fri Oct 18 2013, Francis Wolke
        ;; Photos sorted by most recent (by default)
        raw-photo-data       (-> ds :content first :content)
        photos-essentials    (map #(select-keys (:attrs %) [:farm :server :id :secret]) raw-photo-data)
        photo-urls           (map (fn [{farm-id :farm server-id :server id :id secret :secret}]
                                    (str "http://farm" farm-id ".staticflickr.com/" server-id "/" id "_" secret ".jpg")) photos-essentials)
        photo (loop [photo-urls photo-urls
                     in-question nil]
                (if (and (not (nil? in-question)) (valid-img? (second in-question)))
                  (first in-question)
                  (recur (rest photo-urls)
                         (let [url (first photo-urls)
                               img (ImageIO/read (URL. url))]
                           ;; TODO, Thu Nov 21 2013, Francis Wolke
                           ;; Needs to handle exceptions
                           [url [(.getWidth img) (.getHeight img)]]))))]
    photo))
