(ns wombats.components.configuration
  "Inspired by pointslope/elements' configurator component.

  https://github.com/pointslope/elements/blob/samples/elements/src/pointslope/elements/configurator.clj"
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [immuconf.config :as immuconf]))

;; Private helper functions

(defn- get-app-env
  "Determins the environment that the application is running in.

  Defaults to :dev"
  []
  (keyword (get env :app-env "dev")))

(defn- get-config-files
  "Determins the files that should be used for configuration.

   Defaults -> [config/base.edn, config/{env}.edn]"
  [env]
  (let [base-config "base.edn"
        env-config (str (name env) ".edn")]
    [base-config env-config]))

(defn- build-settings-map
  "Takes a vector of file paths, checks their existance, and runs the remaining
  files through immuconf to build the settings map."
  [files]
  (let [xform (comp
               (map io/resource)
               (remove nil?))
        existing-files (sequence xform (seq files))]
    (apply immuconf/load existing-files)))

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
