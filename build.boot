(def project 'wombats)
(def version "0.1.0-SNAPSHOT")

(set-env! :source-paths #{"src" "test"}
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

                            ;; Database
                            [com.datomic/datomic-free "0.9.5544"]

                            ;; HTTP Server
                            [io.pedestal/pedestal.service "0.5.1"]
                            [io.pedestal/pedestal.jetty   "0.5.1"]

                            ;; HTTP Client
                            [http-kit "2.3.0-alpha1"]

                            ;; Repl reloading
                            [reloaded.repl "0.2.3" :scope "test"]

                            ;; Testing
                            [adzerk/boot-test "1.1.2" :scope "test"]

                            ;; Logging
                            [org.slf4j/jul-to-slf4j     "1.7.21"]
                            [org.slf4j/jcl-over-slf4j   "1.7.21"]
                            [org.slf4j/log4j-over-slf4j "1.7.21"]])

;; Load testing tasks
(require '[adzerk.boot-test :refer :all])

(deftask dev []
  (set-env! :source-paths #(conj % "dev/src"))

  (require 'user))

(deftask refresh-db []
  (require '[datomic.api])

  (let [datomic-uri "datomic:free://localhost:4334/wombats-dev"
        _ (datomic.api/delete-database datomic-uri)
        _ (datomic.api/create-database datomic-uri)
        conn (datomic.api/connect datomic-uri)]
    (datomic.api/transact conn (load-file "resources/datomic/schema.edn"))))
