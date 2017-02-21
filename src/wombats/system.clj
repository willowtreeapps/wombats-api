(ns wombats.system
  (:require [com.stuartsierra.component :as component]
            [reloaded.repl :refer[init start stop go reset]]
            [wombats.routes :as routes]
            [wombats.components.configuration :as config-component]
            [wombats.components.datomic :as datomic-component]
            [wombats.components.service :as service-component]
            [wombats.components.pedestal :as pedestal-component]
            [wombats.components.scheduler :as scheduler-component])
  (:gen-class))

(defn system []
  (component/system-map
   :config (config-component/new-configuration)

   :datomic (component/using
             (datomic-component/new-database)
             [:config])

   :service (component/using
             (service-component/new-service {:new-api-router routes/new-api-router
                                             :new-ws-router routes/new-ws-router})
             [:config :datomic])
   
   :scheduler (component/using
               (scheduler-component/new-scheduler)
               [:config :datomic :service])
   
   :pedestal (component/using
              (pedestal-component/new-pedestal)
              [:config :service])))

(defn -main
  "Entry point for builds"
  []
  (component/start (system)))
