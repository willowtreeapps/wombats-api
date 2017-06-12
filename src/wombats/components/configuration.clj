(ns wombats.components.configuration
  "Inspired by pointslope/elements' configurator component.

  https://github.com/pointslope/elements/blob/samples/elements/src/pointslope/elements/configurator.clj"
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [immuconf.config :as immuconf]))

;; Private helper functions

(defn- get-app-env
  "Determines the environment that the application is running in.

  Defaults to :dev"
  []
  (keyword (get env :app-env "dev")))

(defn- remove-nil
  "Remove nil values from a map"
  [record]
  (into {} (filter (comp some? val) record)))

(defn- get-private-envs
  "Pulls out private envs used in the application and stores them in the
  tmp directory. It the passes them off to immuconf"
  []
  (let [wombat-envs (select-keys env [:wombats-aws-access-key-id
                                      :wombats-aws-secret-key
                                      :wombats-github-client-id
                                      :wombats-github-client-secret
                                      :wombats-signing-secret])
        file-name "/tmp/wombats-config.edn"
        formatted-config {:github (remove-nil {:client-id (:wombats-github-client-id wombat-envs)
                                               :client-secret (:wombats-github-client-secret wombat-envs)})
                          :aws (remove-nil {:access-key-id (:wombats-aws-access-key-id wombat-envs)
                                            :secret-key (:wombats-aws-secret-key wombat-envs)})
                          :security (remove-nil {:signing-secret (:wombats-signing-secret wombat-envs)})}]
    (spit file-name (str formatted-config))
    (io/file file-name)))

(defn- get-private-config-file
  []
  (let [file-location (str (System/getProperty "user.dir") "/config/credentials.edn")]
      (when (.exists (io/as-file file-location))
        file-location)))

(defn- get-config-files
  "Determines the files that should be used for configuration.

   Note: java.io/resource returns nil (if a file is not found) &
         searches for files on the classpate not within the file
         system. immuconf uses slup under the hood which lets you
         specify the ~ user dir.

   Defaults -> [config/base.edn, /config/credentials.edn"
  [env]
  (let [base-config (io/resource "base.edn")
        private-config-envs (get-private-envs)
        private-config-file (get-private-config-file)
        env-config (io/resource (str (name env) ".edn"))]
    (remove nil? [base-config
                  private-config-envs
                  private-config-file
                  env-config])))

(defn- build-settings-map
  "Takes a vector of file paths or configs, checks their existance, and runs the remaining
  files through immuconf to build the settings map."
  [configs]
  (let [settings-map (apply immuconf/load configs)]
    settings-map))

;; Component

(defrecord Configuration [settings env files]
  component/Lifecycle
  (start [component]
    (cond
      ;; If settings exist, the component is already started
      settings component

      ;; If env and files are already set, build up settings
      (and env (seq files)) (assoc component :settings (build-settings-map files))

      ;; Throw error due to miss configuration
      :else (throw
             (ex-info "Missing component properties"
                      {:message "APP_ENV and configuration files are required"
                       :env env
                       :files files}))))
  (stop [component]
    (if-not settings
      component
      (assoc component :settings nil))))

;; Public component function

(defn new-configuration
  "Creates a new system component that manages application configuration.
  This function bootstraps config files by using the immuconf library."
  []
  (let [app-env (get-app-env)]
    (map->Configuration {:env app-env :files (get-config-files app-env)})))
