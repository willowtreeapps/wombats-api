(ns wombats.components.datomic
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [io.rkn.conformity :as c]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]))

;; Private helper functions

(defn- add-auth-to-conn-uri
  [conn-uri access-key-id secret-key]
  (str conn-uri
       "?aws_access_key_id=" access-key-id
       "&aws_secret_key=" secret-key))

(defn- create-db-connection
  [config]
  (let [{conn-uri :uri
         requires-auth :requires-auth} (get-in config [:settings :datomic] {})
        {access-key-id :access-key-id
         secret-key :secret-key} (get-in config [:settings :aws] {})
        datomic-uri (if requires-auth
                      (add-auth-to-conn-uri conn-uri access-key-id secret-key)
                      conn-uri)
        _ (d/create-database datomic-uri)
        conn (d/connect datomic-uri)]

    (log/info (str "Connecting to Datomic at: " conn-uri))

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
