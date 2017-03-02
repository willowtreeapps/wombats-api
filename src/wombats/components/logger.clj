(ns wombats.components.logger
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]))

(defonce ^:private
  appender-map
  {:spit appenders/spit-appender
   :println appenders/println-appender})

(defn- format-appenders
  [config]
  (reduce
   (fn [appenders {appender-type :type
                  appender-options :options}]
     (assoc appenders appender-type ((appender-type appender-map)
                                     appender-options)))
   {} (or (:appenders config)
          [{:type :println
            :options {:stream :auto}}])))

(defn- initialize-logger
  [config]
  (timbre/merge-config! (-> config
                            (assoc :appenders (format-appenders config)))))

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
