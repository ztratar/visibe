(ns eve.state)

(def state (atom {:websocket-connection nil
                  :websocket-functions #{}
                  :trends nil}))

(defn assoc-in-state! [path v]
  (swap! state assoc-in path v))
