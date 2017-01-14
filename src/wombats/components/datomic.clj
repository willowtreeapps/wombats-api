(ns wombats.components.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [io.pedestal.log :as log]))

;; Private helper functions

(defn- create-db-connection
  [config]
  (let [datomic-uri (get-in config [:settings :datomic :uri] nil)
        _ (d/create-database datomic-uri)
        conn (d/connect datomic-uri)]
    (d/transact conn (load-file "resources/datomic/schema.edn"))
    {:conn conn}))

;; Component

(defrecord Datomic [config database]
  component/Lifecycle
  (start [component]
    (if database
      component
      (assoc component :database (create-db-connection config))))
  (stop [component]
    (if-not database
      component
      (do
        (d/release (:conn database))
        (assoc component :database nil)))))

;; Public component methods

(defn new-database
  "Datomic component. Creates database connection to Datomic.

  Note: This component is dependent on the Configuration component."
  []
  (map->Datomic {}))
