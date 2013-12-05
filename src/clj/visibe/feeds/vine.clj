(ns visibe.feeds.vine
  (:require [clj-http.lite.client :as client]))

;; https://github.com/starlock/vino/wiki/API-Reference

;; (client/get "https://api.vineapp.com/timelines/popular")

;; user-agent: com.vine.iphone/1.0.3 (unknown, iPhone OS 6.1.0, iPhone, Scale/2.000000)
;; vine-session-id: <userid>-1231ed86-80a0-4f07-9389-b03199690f73
;; accept-language: en, sv, fr, de, ja, nl, it, es, pt, pt-PT, da, fi, nb, ko, zh-Hans, zh-Hant, ru, pl, tr, uk, ar, hr, cs, el, he, ro, sk, th, id, ms, en-GB, ca, hu, vi, en-us;q=0.8
