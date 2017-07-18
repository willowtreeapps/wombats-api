(def boot-constants
  {:task-confirm "Are you sure you want to %s the %s database?
Type \"Yes\" to confirm."
   :task-block "You cannot run %s on the %s database."
   :task-cancel "Did not %s the %s database."})

;; Environment Permission Variables
(defonce refresh-perm "refresh")
(defonce delete-perm "delete")
(defonce seed-perm "seed")
(defonce refresh-fn-perm "refresh-db-functions")

(def main-dependencies
  '[;; Core Clojure libs
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
   [com.amazonaws/aws-java-sdk-dynamodb "1.11.82"]
   [io.rkn/conformity "0.4.0"]

   ;; Amazon SDK
   [com.amazonaws/aws-java-sdk "1.11.82"]

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
   [tolitius/boot-check "0.1.4" :scope "test"]])

(def datomic-free
  '[[com.datomic/datomic-free "0.9.5561.50"]])

(def datomic-pro
  '[[com.datomic/datomic-pro "0.9.5561.50"]])

(defn- get-wombats-env
  "Gets the user defined wombats environment to determine dependencies"
  []
  (let [env (System/getenv "WOMBATS_ENV")]
    (if (or (= env "")
            (nil? env))
      "dev"
      env)))

(defn- get-wombats-db-name
  "Gets the user readable name of the database that various tasks will be performed on"
  []
  (let [wombats-env (get-wombats-env)]
    (case wombats-env
      "dev" "local"
      "dev-ddb" "development"
      "qa-ddb" "qa"
      "prod-ddb" "production")))

(defn- get-env-permissions
  "Used by can-run-command to determine if various functions can be called on the specified db"
  []
  (case (get-wombats-env)
    "dev" [refresh-perm delete-perm seed-perm refresh-fn-perm]
    "dev-ddb" [refresh-perm delete-perm seed-perm refresh-fn-perm]
    "qa-ddb" [refresh-perm delete-perm seed-perm refresh-fn-perm]
    "prod-ddb" []))

(defn- can-run-command?
  "Uses environment to check whether a command string can be run"
  [cmdstring]
  (not= (some #(= cmdstring %) (get-env-permissions))
            nil))

(defn- get-dependencies
  "Checks environment to determine whether to load datomic free or pro"
  []
  (if (= (get-wombats-env) "dev")
    (into main-dependencies datomic-free)
    (into main-dependencies datomic-pro)))

(defn- is-local?
  "Check if current env is local or local-ddb for loading user"
  []
  (let [env (get-wombats-env)]
    (or (= env "dev")
        (= env "dev-ddb"))))

(defn- get-source-paths
  "Build source path based on whether running in dev or qa/prod"
  []
  (let [src-test #{"src" "test"}]
    (if (is-local?)
      (conj src-test "dev/src")
      src-test)))

(defn- load-user
  "If loading in dev, load user as well"
  []
  (when (is-local?)
    (require 'user)))

(set-env! :project 'wombats
          :version "1.0.0-alpha1"
          :source-paths (get-source-paths)
          :resource-paths #{"src" "resources" "config"}
          :dependencies (get-dependencies)
          :repositories #(conj % ["my-datomic" {:url "https://my.datomic.com/repo"
                                                :username
                                                (System/getenv "DATOMIC_USERNAME")
                                                :password
                                                (System/getenv "DATOMIC_PASSWORD")}])
          :target-path "target")

(load-user)

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

(defn- get-datomic-uri
  [{{uri :uri
     auth? :requires-auth} :datomic}
   {{akid :access-key-id
     sk :secret-key} :aws}]

  (if auth?
    (str uri "?aws_access_key_id=" akid "&aws_secret_key=" sk)
    uri))

(defn- get-private-config-file
  []
  (let [file-location-dev (str (System/getProperty "user.dir") "/config/config.edn")
        file-location-prod (str (System/getProperty "user.home") "/.wombats/config.edn")]
    (when (.exists (io/as-file file-location-dev))
      file-location-dev)
    (when (.exists (io/as-file file-location-prod))
      file-location-prod)))

(defn- build-connection-string
  []
  (let [env (get-wombats-env)
        env-settings (-> (str env ".edn")
                         (clojure.java.io/resource)
                         (clojure.java.io/file)
                         (slurp)
                         (clojure.edn/read-string))
        config-settings (load-file (get-private-config-file))]
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

(defn- task-constructor
  [task-name func]
  (let [db (get-wombats-db-name)]
    (if (can-run-command? task-name)
      (do
        (println (format (boot-constants :task-confirm) task-name db))
        (if (= (read-line) "Yes")
          (func)
          (println (format (boot-constants :task-cancel) task-name db))))
      (println (format (boot-constants :task-block) task-name db)))))

(deftask refresh-db-functions
  "resets the transactors in the db"
  []
  (task-constructor "refresh-db-functions"
                    #(-> (build-connection-string)
                         (d/connect)
                         (db-fns/seed-database-functions))))

(deftask seed
  "Seed the current database set through WOMBATS_ENV"
  []
  (task-constructor "seed"
                    #(-> (build-connection-string)
                         create-db!
                         seed-db!)))

(deftask refresh
  "Refresh the current database set through WOMBATS_ENV"
  []
  (task-constructor "refresh"
                    #(-> (build-connection-string)
                          refresh-db!)))

(deftask delete
  "Deletes the current database set through WOMBATS_ENV"
  []
  (task-constructor "delete"
                    #(-> (build-connection-string)
                          delete-db!)))

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
