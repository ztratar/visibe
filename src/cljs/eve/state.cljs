(ns eve.state)

(def state (atom {:websocket-connection nil
                  :websocket-functions #{}
                  :trends {}}))

(defn feed-watch [key identify old new]
  (.log js/console (apply str [key old "=>" new])))

(add-watch state :feed feed-watch)

(defn update-in-state!
  ([path f] (swap! state update-in path f))
  ([path f x] (swap! state update-in path f x)))

(defn assoc-in-state! [path v]
  (swap! state assoc-in path v))
