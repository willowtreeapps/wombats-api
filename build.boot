(def project 'wombats)
(def version "0.1.0-SNAPSHOT")

(set-env! :source-paths #{"src"}
          :resource-paths #{"resources" "config"}
          :dependencies   '[;; Core Clojure libs
                            [org.clojure/clojure   "1.9.0-alpha14"]
                            [org.clojure/data.json "0.2.6"]

                            ;; JSON Parsing
                            [cheshire "5.7.0"]

                            ;; Environment configuration
                            [environ         "1.1.0"]
                            [levand/immuconf "0.1.0"]

                            ;; Component lifecycle management
                            [com.stuartsierra/component   "0.3.2"]

                            ;; Repl reloading
                            [reloaded.repl "0.2.3" :scope "test"]

                            ;; Database
                            [com.datomic/datomic-free "0.9.5544"]

                            ;; HTTP Server
                            [io.pedestal/pedestal.service "0.5.1"]
                            [io.pedestal/pedestal.jetty   "0.5.1"]
                            ;; [io.pedestal/pedestal.immutant "0.5.1"]

                            ;; HTTP Client
                            [http-kit "2.3.0-alpha1"]

                            ;; Logging
                            [org.slf4j/jul-to-slf4j     "1.7.21"]
                            [org.slf4j/jcl-over-slf4j   "1.7.21"]
                            [org.slf4j/log4j-over-slf4j "1.7.21"]])

(deftask dev []
  (set-env! :source-paths #(conj % "dev/src"))

  (require 'user))
