(defproject wombats "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [;; Core
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40" :scope "provided"]

                 [base64-clj "0.1.1"]

                 ;; Codec implementations
                 [org.clojure/data.codec "0.1.0"]

                 ;;
                 ;; API Libs
                 ;;

                 ;; Server / Server Middleware
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring-transit "0.1.4"]
                 [ring/ring-json "0.4.0"]
                 [ring-server "0.4.0"]
                 [http-kit "2.1.19"]
                 [ring-cors "0.1.8"]

                 ;; Routing
                 [compojure "1.5.0"]

                 ;; Authenication / Authorization
                 [buddy/buddy-auth "0.13.0"]
                 [buddy/buddy-hashers "0.14.0"]

                 ;; Configuration using environment variables and EDN configuration files
                 [yogthos/config "0.8"]

                 ;; Logging
                 [com.taoensso/timbre "4.4.0"]

                 ;; Socket Support
                 [com.taoensso/sente "1.9.0-beta2"]
                 ;; [com.taoensso/sente "1.8.1"]

                 ;; Schema / Validation
                 [prismatic/schema "1.1.1"]

                 ;; JSON Parsing
                 [cheshire "5.1.1"]

                 ;; Database Drivers
                 [com.novemberain/monger "3.0.2"]

                 ;; Lifecycle Service Management
                 [com.stuartsierra/component "0.3.1"]

                 ;;
                 ;; Client Libs
                 ;;

                 ;; Clojure & React
                 [re-frame "0.7.0"]
                 [reagent "0.5.1" :exclusions [org.clojure/tools.reader]]
                 [reagent-utils "0.1.7"]

                 ;; Routing
                 [secretary "1.2.3"]
                 [venantius/accountant "0.1.7" :exclusions [org.clojure/tools.reader]]

                 ;; Rendering HTML in ClojureScript
                 [hiccup "1.0.5"]

                 ;; XHR Support
                 [cljs-ajax "0.5.4"]

                 ;; Promise Lib
                 [funcool/promesa "1.3.1"]

                 ;; Makes working with URLS dead simple
                 [com.cemerick/url "0.1.1"]

                 ;; LocalStorage Support
                 [alandipert/storage-atom "2.0.1"]]

  :plugins [[lein-environ "1.0.2"]
            [lein-cljsbuild "1.1.1"]
            [lein-less "1.7.5"]
            [jonase/eastwood "0.2.3"]
            [lein-cloverage "1.0.6"]
            [lein-asset-minifier "0.2.7" :exclusions [org.clojure/clojure]]]

  :less {:source-paths ["src/less"]
         :target-path  "resources/public/css"}

  :ring {:handler wombats.handler/app
         :uberwar-name "wombats.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "wombats.jar"

  :main wombats.server

  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir]
                                                 [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :test-paths ["tests"]

  :minify-assets {:assets {"resources/public/css/main.min.css" "resources/public/css/main.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc"]
                             :compiler {:output-to "target/cljsbuild/public/js/app.js"
                                        :output-dir "target/cljsbuild/public/js/out"
                                        :asset-path   "/js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns wombats.repl}

                   :dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.4.0"]
                                  [prone "1.1.1"]
                                  [lein-figwheel "0.5.2" :exclusions [org.clojure/core.memoize
                                                                      ring/ring-core
                                                                      org.clojure/clojure
                                                                      org.ow2.asm/asm-all
                                                                      org.clojure/data.priority-map
                                                                      org.clojure/tools.reader
                                                                      org.clojure/clojurescript
                                                                      org.clojure/core.async
                                                                      org.clojure/tools.analyzer.jvm]]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [pjstadig/humane-test-output "0.8.0"]]

                   :source-paths ["env/dev/clj"]

                   :resource-paths ["tests" "test-resources"]

                   :plugins [[lein-figwheel "0.5.2" :exclusions [org.clojure/core.memoize
                                                                 ring/ring-core
                                                                 org.clojure/clojure
                                                                 org.ow2.asm/asm-all
                                                                 org.clojure/data.priority-map
                                                                 org.clojure/tools.reader
                                                                 org.clojure/clojurescript
                                                                 org.clojure/core.async
                                                                 org.clojure/tools.analyzer.jvm]]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :nrepl-port 7002
                              :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
                              :css-dirs ["resources/public/css"]
                              :ring-handler wombats.router/app}

                   :env {:dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "wombats.dev"
                                                         :source-map true}}}}}

             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once"]]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app {:source-paths ["env/prod/cljs"]
                                                  :compiler {:optimizations :advanced
                                                             :pretty-print false}}}}}})
