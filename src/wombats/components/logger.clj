(ns wombats.components.logger
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]))

;; Setup Timbre
(timbre/refer-timbre)

(defn- initialize-logger
  [config]
  (timbre/merge-config! config))

;; Component

(defrecord Logger [config logger]
  component/Lifecycle
  (start [component]
    (if logger
      component
      (assoc component
             :logger
             (initialize-logger (get-in config [:settings :logger])))))
  (stop [component]
    (if-not logger
      component
      (assoc component :logger nil))))

;; Public component methods

(defn new-logger
  []
  (map->Logger {}))
