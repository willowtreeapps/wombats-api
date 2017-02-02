(ns wombats.components.service
  (:require [com.stuartsierra.component :as component]))

;; Private helper functions

(defn- bootstrap-service
  [{:keys [] :as config}
   {:keys [new-api-router new-ws-router] :as route-map}
   service-dependencies]
  {:api-routes (new-api-router service-dependencies)
   :ws-routes (new-ws-router service-dependencies)})

;; Component

(defrecord Service [config datomic route-map service]
  component/Lifecycle
  (start [component]
    (if service
      component
      (assoc component :service (bootstrap-service config
                                                   route-map
                                                   {:datomic datomic
                                                    :github (get-in config [:settings
                                                                            :github])
                                                    :aws (get-in config [:settings
                                                                         :aws])}))))
  (stop [component]
    (if-not service
      component
      (assoc component :service nil))))

;; Public component functions

(defn new-service
  [route-map]
  (map->Service {:route-map route-map}))
