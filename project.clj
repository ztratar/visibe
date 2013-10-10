(defproject visibe "0.1.0-SNAPSHOT"

  :description "..."

  :url "http://visibe.com"

  :source-paths ["src/clj"]

  :dependencies [
                 [cheshire "5.2.0"]
                 [clj-http-lite "0.2.0"]
                 [clj-time "0.6.0"]
                 [cljs-painkiller "0.1.0"]
                 [com.cemerick/piggieback "0.1.0" :exclusions [org.clojure/data.json]]
                 [com.cemerick/pomegranate "0.2.0"]
                 [com.draines/postal "1.11.0"]
                 [com.keminglabs/c2 "0.2.2"]
                 [com.novemberain/monger "1.6.0"]
                 [twitter-api "0.7.4" :exclusions [org.clojure/data.json]]
                 [org.clojure/data.codec "0.1.0"]
                 [compojure "1.1.5" :exclusions [ring/ring-core]]
                 [instagram-api "0.1.6" :exclusions [org.clojure/data.json]]
                 [criterium "0.4.2"]
                 [domina "1.0.2-SNAPSHOT"]
                 [garden "0.1.0-beta6"]
                 [hiccup "1.0.3"]
                 [http-kit "2.1.4"]
                 [org.clojars.runa/clj-schema "0.9.4"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1889"]
                 [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]
                 [org.clojure/core.logic "0.8.3"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/tools.reader "0.7.7"]
                 [org.clojure/tools.trace "0.7.5"]
                 [org.toomuchcode/clara-rules "0.1.0"]
                 [prismatic/dommy "0.1.1"]
                 [prismatic/hiphip "0.1.0"]
                 [prismatic/plumbing "0.1.0"]
                 [rhizome "0.1.8"]
                 [ring-refresh "0.1.1"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [ring/ring-json "0.2.0"]
                 [shoreleave/shoreleave-remote "0.3.0"]
                 [shoreleave/shoreleave-remote-ring "0.3.0"]
                 ]

  :main visibe.core/main-

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :injections [(require '[cljs.repl.browser :as brepl]
                        '[cemerick.piggieback :as pb])
               (defn browser-repl []
                 (pb/cljs-repl :repl-env (brepl/repl-env :port 8002)))]

  :plugins [[lein-cljsbuild "0.3.0"]]

  :cljsbuild {:repl-listen-port 8002
              
              :builds {:prod {:source-paths ["src/cljs"]
                              :compiler {:output-to "resources/public/js/visibe.js"}
                              :optimizations :advanced}

                       :pre-prod {:source-paths ["src/cljs"]
                                  :compiler {:output-to "resources/public/js/visibe_pre.js"}
                                  :optimizations :simple}

                       :dev {:pretty-print true
                             :source-paths ["src/cljs"]
                             :externs ["lib/d3.v3.js"]
                             :compiler {:output-to "resources/public/js/visibe_dbg.js"}
                             :optimizations :whitespace}}})
