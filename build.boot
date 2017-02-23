(set-env! :project 'wombats
          :version "0.1.0-SNAPSHOT"
          :source-paths #{"src" "test"}
          :resource-paths #{"src" "resources" "config"}
          :dependencies   '[;; Core Clojure libs
                            [org.clojure/clojure   "1.9.0-alpha14" :scope "provided"]
                            [org.clojure/data.json "0.2.6"]

                            ;; High-performance serialization library
                            [com.taoensso/nippy "2.12.2"]

                            ;; JSON Parsing
                            [cheshire "5.7.0"]

                            ;; Base64 Decoding
                            [base64-clj "0.1.1"]

                            ;; Working with time
                            [clj-time "0.13.0"]

                            ;; Scheduler
                            [jarohen/chime "0.2.0"]

                            ;; Token Support
                            [buddy "1.3.0"]

                            ;; Environment configuration
                            [environ         "1.1.0"]
                            [levand/immuconf "0.1.0"]

                            ;; Component lifecycle management
                            [com.stuartsierra/component   "0.3.2"]

                            ;; Database
                            [com.datomic/datomic-pro "0.9.5554"]
                            [com.amazonaws/aws-java-sdk-dynamodb "1.11.6"]
                            [io.rkn/conformity "0.4.0"]

                            ;; Amazon SDK
                            [com.amazonaws/aws-java-sdk "1.11.6"]

                            ;; HTTP Server
                            [io.pedestal/pedestal.service "0.5.2"]
                            [io.pedestal/pedestal.jetty   "0.5.2"]
                            [io.pedestal/pedestal.interceptor "0.5.2"]

                            ;; HTTP Client
                            [http-kit "2.3.0-alpha1"]

                            ;; Repl reloading
                            [reloaded.repl "0.2.3" :scope "test"]

                            ;; Testing
                            [adzerk/boot-test "1.1.2" :scope "test"]

                            ;; Code Analysis
                            [tolitius/boot-check "0.1.4" :scope "test"]

                            ;; Logging
                            [org.slf4j/jul-to-slf4j     "1.7.21"]
                            [org.slf4j/jcl-over-slf4j   "1.7.21"]
                            [org.slf4j/log4j-over-slf4j "1.7.21"]]
          :repositories #(conj % ["my-datomic" {:url "https://my.datomic.com/repo"
                                                :username (System/getenv "DATOMIC_USERNAME")
                                                :password (System/getenv "DATOMIC_PASSWORD")}])
          :target-path "target")

(task-options!
 pom {:project (get-env :project)
      :version (get-env :version)}
 aot {:namespace '#{wombats.system}}
 jar {:main 'wombats.system})

;; Load testing tasks
(require '[adzerk.boot-test :refer :all])

;; Load datomic Move this task into its own file
(require '[datomic.api :as d])

;; Load code analysis tasks
(require '[tolitius.boot-check :as check])

(deftask dev []
  (set-env! :source-paths #(conj % "dev/src"))

  (require 'user))

(deftask dev-ddb
  "Start"[]
  (set-env! :source-paths #(conj % "dev/src"))
  (System/setProperty "APP_ENV" "dev-ddb")

  (require 'user))

(defn- get-datomic-uri
  [{{uri :uri
     auth? :requires-auth} :datomic}
   {{akid :access-key-id
     sk :secret-key} :aws}]

  (if auth?
    (str uri "?aws_access_key_id=" akid "&aws_secret_key=" sk)
    uri))

(defn- build-connection-string
  []
  (let [env (System/getProperty "APP_ENV")
        env-settings (-> (str env ".edn")
                         (clojure.java.io/resource)
                         (clojure.java.io/file)
                         (slurp)
                         (clojure.edn/read-string))
        config-settings (load-file
                         (str (System/getProperty "user.home") "/.wombats/config.edn"))]
    (get-datomic-uri env-settings config-settings)))

(defn- add-schema
  "Initializes the DB with the proper schema"
  [conn]
  (d/transact conn (load-file "resources/datomic/schema.edn")))

(defn- add-roles
  "Adds Wombat roles to the DB"
  [conn]
  (d/transact conn (load-file "resources/datomic/roles.edn")))

(defn- seed-users
  "Seeds the DB with users (mainly to support seeding with roles)"
  [conn]
  (d/transact conn (load-file "resources/datomic/users.edn")))

(defn- seed-simulator-templates
  "Seeds the DB with simulator templates"
  [conn]
  (require '[wombats.daos.helpers])
  (require '[taoensso.nippy])

  (->> (load-file "resources/datomic/simulator-templates.edn")
       (map #(-> %
                 (assoc :simulator-template/id (wombats.daos.helpers/gen-id))
                 (update :simulator-template/arena taoensso.nippy/freeze)))
       (d/transact conn)))

(defn- refresh-db!
  [uri]
  (d/delete-database uri)
  (d/create-database uri)
  (let [conn (d/connect uri)]
    @(add-schema conn)
    @(add-roles conn)
    @(seed-users conn)
    @(seed-simulator-templates conn)))

(deftask refresh-dev-ddb
  "Resets the dev dynamo db"
  []
  (System/setProperty "APP_ENV" "dev-ddb")

  (-> (build-connection-string)
      refresh-db!))

(deftask refresh-qa-ddb
  "Resets the dev dynamo db"
  []
  (System/setProperty "APP_ENV" "qa-ddb")

  (-> (build-connection-string)
      refresh-db!))

(deftask refresh-db
  "resets the database"
  []
  (System/setProperty "APP_ENV" "dev")

  (-> (build-connection-string)
      refresh-db!))

(deftask build
  "Creates a new build"
  []
  (def jar-file (format "%s.jar" (get-env :project)))

  (comp
   (aot)
   (pom)
   (uber)
   (jar :file jar-file)
   (sift :include #{(re-pattern jar-file)})
   (zip :file (format "%s-%s.zip"
                      (get-env :project)
                      (get-env :version)))
   (target)))

(deftask check-sources
  "Analyzes source code"
  []
  (comp
   (check/with-eastwood)))
