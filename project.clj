(defproject visibe "0.1.0-SNAPSHOT"

  :description "..."

  :url "http://visibe.com"

  :source-paths ["src/clj"]

  :dependencies [
                 [cheshire "5.2.0"]
                 [clj-http-lite "0.2.0"]
                 [clj-time "0.6.0"]
                 [cljs-painkiller "0.1.0"]
                 [com.cemerick/piggieback "0.1.2" :exclusions [org.clojure/data.json]]
                 [com.cemerick/pomegranate "0.2.0"]
                 [com.draines/postal "1.11.0"]
                 [com.andrewmcveigh/cljs-time "0.1.1"]
                 [io.aviso/pretty "0.1.1"]
                 [net.drib/strokes "0.5.1"]
                 [faker "0.2.2"]
                 [com.keminglabs/c2 "0.2.2"]
                 [com.novemberain/monger "1.6.0"]
                 [twitter-api "0.7.4" :exclusions [org.clojure/data.json]]
                 [org.clojure/data.codec "0.1.0"]
                 [compojure "1.1.5" :exclusions [ring/ring-core]]
                 [instagram-api "0.1.6" :exclusions [org.clojure/data.json]]
                 [net.drib/mrhyde "0.5.3"]
                 [org.clojure/core.match "0.2.0"]                 
                 [org.clojure/core.incubator "0.1.3"]
                 [criterium "0.4.2"]
                 [shodan "0.1.0"]
                 [com.flickr4java/flickr "2.5"]
                 [domina "1.0.2-SNAPSHOT"]
                 [garden "1.1.3"]
                 [image-resizer "0.1.6"]
                 [hiccup "1.0.3"]
                 [http-kit "2.1.5"]
                 [org.clojars.runa/clj-schema "0.9.4"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2060"]
                 [com.cemerick/url "0.1.0"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [org.clojure/core.logic "0.8.3"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/tools.trace "0.7.5"]
                 [org.toomuchcode/clara-rules "0.1.0"]
                 [cljs-http "0.1.0"]
                 [prismatic/dommy "0.1.1"]
                 [clojure-opennlp "0.3.1"] ;; uses Opennlp 1.5.3
                 [secretary "0.4.0"]
                 [prismatic/hiphip "0.1.0"]
                 [prismatic/plumbing "0.1.0"]
                 [rhizome "0.1.8"]
                 [org.clojure/data.xml "0.0.7"]
                 [ring-refresh "0.1.1"]
                 [clj-wamp "1.0.0-rc1"]
                 [org.clojure/core.match "0.2.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [ring/ring-json "0.2.0"]
                 [shoreleave/shoreleave-browser "0.3.0"]
                 [shoreleave/shoreleave-core "0.3.0"]
                 ]

  :main visibe.core/main-

  :jvm-opts ["-Djava.awt.headless=true"]

  :repositories [["JCenter" {:url "http://jcenter.bintray.com"
                             :snapshots false}]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :injections [(require '[cljs.repl.browser :as brepl]
                        '[cemerick.piggieback :as pb])
               (defn browser-repl []
                 (pb/cljs-repl :repl-env (brepl/repl-env :port 8002)))]

  :plugins [[lein-cljsbuild "1.0.0-alpha2"]
            [codox "0.6.6"]]

  :cljsbuild  {:repl-listen-port 8002
               :builds [{:id "dev"
                         :source-paths ["src/cljs"]
                         :compiler {:output-to "resources/public/js/out/goog/eve.js"
                                    :output-dir "resources/public/js/out"
                                    :optimizations :none
                                    :source-map true}}]})
