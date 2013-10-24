(ns ^{:doc "..."}
  visibe.feeds.instagram
  (:use instagram.oauth
        instagram.callbacks
        instagram.callbacks.handlers
        instagram.api.endpoint)
  (:import instagram.callbacks.protocols.SyncSingleCallback))

(search-media)
(get-media )


;; ztratar
;; qvqzqOinWrbNN64B

;;; All requests must be made over SSL.

(def ^:dynamic *creds* (make-oauth-creds *client-id*
                                         *client-secret*
                                         *redirect-uri*))

; Generate the authorization url
(def ^:dynamic *auth-url* (authorization-url *creds* "likes comments relationships"))

; Exchange the code to get the user's access token
(let [access-token (-> (get-access-token *creds* "code-from-IG") :body :access_token)]
                                        ; do stuff with access-token, save it somewhere etc.
  (println access-token))

; You can make unauthentificated calls without access token, but you
; still needs to send your app credentials. Some API calls won't work without
; an access token, check the Instagram documentation.

(get-popular :oauth *creds*)

; The same API call, to get popular photos, but with the user’s access token.

(get-popular :access-token *access-token*)

; Some endpoints require parameters, see instagram.api.endpoint and the Instagram
; documentation. Here are some examples:

; Search a user
(search-users :access-token *access-token* :params {:q "rydgel"})

; Get medias from an user
(get-user-medias :access-token *access-token* :params {:user_id "36783"})




