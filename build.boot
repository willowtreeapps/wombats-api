(set-env! :project 'wombats
          :version "1.0.0-alpha1"
          :source-paths #{"src" "test"}
          :resource-paths #{"src" "resources" "config"}
          :dependencies   '[;; Core Clojure libs
                            [org.clojure/clojure   "1.9.0-alpha14" :scope "provided"]
                            [org.clojure/data.json "0.2.6"]

                            ;; High-performance serialization library
                            [com.taoensso/nippy "2.12.2"]

                            ;; Logging
                            [com.taoensso/timbre "4.8.0"]
                            [org.slf4j/jul-to-slf4j     "1.7.21"]
                            [org.slf4j/jcl-over-slf4j   "1.7.21"]
                            [org.slf4j/log4j-over-slf4j "1.7.21"]

                            ;; Extended core library for Clojure
                            [com.taoensso/encore "2.89.0"]

                            ;; Stringify-ing code
                            [serializable-fn "1.1.4"]

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

                            ;; URL util lib
                            [com.cemerick/url "0.1.1"]

                            ;; Repl reloading
                            [reloaded.repl "0.2.3" :scope "test"]

                            ;; Testing
                            [adzerk/boot-test "1.1.2" :scope "test"]

                            ;; Code Analysis
                            [tolitius/boot-check "0.1.4" :scope "test"]]
          :repositories #(conj % ["my-datomic" {:url "https://my.datomic.com/repo"
                                                :username (System/getenv "DATOMIC_USERNAME")
                                                :password (System/getenv "DATOMIC_PASSWORD")}])
          :target-path "target")

(task-options!
 pom {:project (get-env :project)
      :version (get-env :version)}
 aot {:namespace '#{wombats.system}}
 jar {:main 'wombats.system})

;; Require io
(require '[clojure.java.io :as io])

;; Load testing tasks
(require '[adzerk.boot-test :refer :all])

;; Load datomic Move this task into its own file
(require '[datomic.api :as d])

;; Load code analysis tasks
(require '[tolitius.boot-check :as check])

;; Load nippy for seed tasks
(require '[taoensso.nippy])

(require '[wombats.daos.helpers])
(require '[wombats.arena.core])

(require '[wombats.datomic.db-functions :as db-fns])

(defn read-all
  "Read all forms in f, where f is any resource that can
   be opened by io/reader"
  [f]
  (datomic.Util/readAll (io/reader f)))

(defn transact-all
  "Load and run all transactions from f, where f is any
   resource that can be opened by io/reader."
  [conn f]
  (doseq [txd (read-all f)]
    @(d/transact conn txd))
  :done)

(deftask dev []
  (set-env! :source-paths #(conj % "dev/src"))

  (require 'user))

(deftask dev-ddb
  []
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

(defn- lookup-arena-ref
  [arena-name conn]
  (wombats.daos.helpers/get-entity-id conn :arena/name arena-name))

(defn- generate-simulator-arena
  [{:keys [:simulator-template/arena-template] :as template}
   conn]
  (let [arena-config (wombats.daos.helpers/get-entity-by-prop conn :arena/name arena-template)]
    (-> template
        (assoc :simulator-template/arena (wombats.arena.core/generate-arena arena-config))
        (update :simulator-template/arena taoensso.nippy/freeze))))

(defn- seed-simulator-templates
  "Seeds the DB with simulator templates"
  [conn]
  (->> (read-all "resources/datomic/simulator-templates.dtm")
       (first)
       (map #(-> %
                 (assoc :simulator-template/id (wombats.daos.helpers/gen-id))
                 ;; NOTE This must be run prior to update arena-template until I figure out why
                 ;; I cannot query datomic using the :db/id ref that lookup-arena-ref returns
                 (generate-simulator-arena conn)
                 (update :simulator-template/arena-template lookup-arena-ref conn)))
       (d/transact conn)))

(defn- seed-db!
  [uri]
  (let [conn (d/connect uri)]
    @(db-fns/seed-database-functions conn)
    (transact-all conn "resources/datomic/schema.dtm")
    (transact-all conn "resources/datomic/roles.dtm")
    (transact-all conn "resources/datomic/users.dtm")
    (transact-all conn "resources/datomic/arena-templates.dtm")
    (transact-all conn "resources/datomic/access-keys.dtm")
    @(seed-simulator-templates conn))
  uri)

(defn- create-db!
  [uri]
  (d/create-database uri)
  uri)

(defn- delete-db!
  [uri]
  (d/delete-database uri)
  uri)

(defn- refresh-db!
  [uri]
  (-> uri
      delete-db!
      create-db!
      seed-db!))

(deftask refresh-db-functions
  "resets the transactors in the db"
  []
  (System/setProperty "APP_ENV" "dev")
  (-> (build-connection-string)
      (d/connect)
      (db-fns/seed-database-functions)))

(deftask seed-local
  "Seeds the dev dynamo db"
  []
  (System/setProperty "APP_ENV" "dev")

  (-> (build-connection-string)
      create-db!
      seed-db!))

(deftask refresh-local
  "resets the database"
  []
  (System/setProperty "APP_ENV" "dev")

  (-> (build-connection-string)
      refresh-db!))

(deftask delete-local
  "deletes the local db"
  []
  (System/setProperty "APP_ENV" "dev")

  (-> (build-connection-string)
      delete-db!))

(deftask seed-dev
  "Seeds the dev dynamo db"
  []
  (System/setProperty "APP_ENV" "dev-ddb")

  (-> (build-connection-string)
      create-db!
      seed-db!))

(deftask refresh-dev
  "Resets the dev dynamo db"
  []
  (System/setProperty "APP_ENV" "dev-ddb")

  (-> (build-connection-string)
      refresh-db!))

(deftask delete-dev
  []
  (System/setProperty "APP_ENV" "dev-ddb")

  (-> (build-connection-string)
      delete-db!))

(deftask seed-qa
  "Seeds the dev dynamo db"
  []
  (System/setProperty "APP_ENV" "qa-ddb")

  (-> (build-connection-string)
      create-db!
      seed-db!))

(deftask refresh-qa-ddb
  "Resets the qa dynamo db"
  []
  (System/setProperty "APP_ENV" "qa-ddb")

  (-> (build-connection-string)
      refresh-db!))

(deftask delete-qa
  []
  (System/setProperty "APP_ENV" "qa-ddb")

  (-> (build-connection-string)
      delete-db!))

#_(deftask seed-prod
  "Seeds the prod dynamo db"
  []
  (System/setProperty "APP_ENV" "prod-ddb")

  (-> (build-connection-string)
      create-db!
      seed-db!))

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
