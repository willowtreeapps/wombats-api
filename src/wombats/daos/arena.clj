(ns wombats.daos.arena
  (:require [datomic.api :as d]
            [wombats.daos.helpers :refer [get-entity-by-prop
                                          get-entities-by-prop
                                          get-entity-id
                                          retract-entity-by-prop]]
            [wombats.handlers.helpers :refer [wombat-error]]))

(defn- get-arena-entity-id
  "Returns the entity id of an arena given the public arena id"
  [conn arena-id]
  (get-entity-id conn :arena/id arena-id))

(defn get-arenas
  "Returns a seq of arena configurations"
  [conn]
  (fn []
    (get-entities-by-prop conn :arena/id)))

(defn get-arena-by-name
  "Returns an arena configuration matching a given name"
  [conn]
  (fn [name]
    (get-entity-by-prop conn :arena/name name)))

(defn get-arena-by-id
  "Returns an arena configuration matching a given arena id"
  [conn]
  (fn [id]
    (get-entity-by-prop conn :arena/id id)))

(defn- ensure-name-availability
  "Ensure the name we want to set for an arena is available"
  [conn name]
  (let [existing-arena ((get-arena-by-name conn) name)]
    (when existing-arena
      (wombat-error {:code 100000
                     :params [name]}))))

(defn add-arena
  "Sends a new arena configuration to the transactor"
  [conn]
  (fn [arena-config]

    (ensure-name-availability conn (:arena/name arena-config))

    (d/transact-async conn [(merge {:db/id (d/tempid :db.part/user)}
                                   arena-config)])))

(defn update-arena
  [conn]
  (fn [{:keys [:arena/id
              :arena/name] :as arena-config}]

    (let [current-arena ((get-arena-by-id conn) id)]

      (when-not current-arena
        (wombat-error {:code 100001
                       :details {:arena-id id}}))

      (when-not (= (:arena/name current-arena) name)
        (ensure-name-availability conn name))

      (d/transact-async conn [arena-config]))))

(defn retract-arena
  "Retracts an arena configuration"
  [conn]
  (fn [arena-id]
    (retract-entity-by-prop conn :arena/id arena-id)))
