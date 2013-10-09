(ns visibe.feeds.storage-test
  (:use clojure.test)
  (:require [visibe.feeds.storage :refer :all]
            [visibe.schemas :refer :all]
            [monger.collection :as c]
            [monger.core :as mg]))

(deftest trend-and-datum-storage
  
  ;; localhost, default port
  (mg/connect!)
  (c/remove "trends")

  (let [tweets (n-sorted-tweets 10)
        [old new] ((juxt (partial take 5) (partial drop 5)) tweets)]
    
    (create-trend "test-trend")
    (append-datums "test-trend" old)
    (append-datums "test-trend" new)
    
    (is (= (butlast old) (previous-50-datums "test-trend" (last old)))) 
    (is (= (rest new) (after-datum "test-trend" (first new))))))
