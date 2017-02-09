(ns wombats.daos.arena
  (:require [datomic.api :as d]
            [wombats.daos.helpers :refer [db-requirement-error
                                          get-entity-by-prop
                                          get-entities-by-prop
                                          get-entity-id
                                          retract-entity-by-prop]]))

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
  (let [existing-arena? ((get-arena-by-name conn) name)]
    (when existing-arena?
      (db-requirement-error (str "Arena " name " already exists.")))))

(defn add-arena
  "Sends a new arena configuration to the transactor"
  [conn]
  (fn [{:keys [:arena/name
              :arena/id
              :arena/width
              :arena/height
              :arena/shot-damage
              :arena/smoke-duration
              :arena/food
              :arena/poison
              :arena/steel-walls
              :arena/steel-wall-hp
              :arena/wood-walls
              :arena/wood-wall-hp
              :arena/zakano
              :arena/zakano-hp
              :arena/wombat-hp
              :arena/perimeter]}]

    (ensure-name-availability conn name)

    (d/transact-async conn [{:db/id (d/tempid :db.part/user)
                             :arena/id id
                             :arena/name name
                             :arena/width width
                             :arena/height height
                             :arena/shot-damage shot-damage
                             :arena/smoke-duration smoke-duration
                             :arena/food food
                             :arena/poison poison
                             :arena/steel-walls steel-walls
                             :arena/steel-wall-hp steel-wall-hp
                             :arena/wood-walls wood-walls
                             :arena/wood-wall-hp wood-wall-hp
                             :arena/zakano zakano
                             :arena/zakano-hp zakano-hp
                             :arena/wombat-hp wombat-hp
                             :arena/perimeter perimeter}])))

(defn update-arena
  [conn]
  (fn [{:keys [:arena/name
              :arena/id
              :arena/width
              :arena/height
              :arena/shot-damage
              :arena/smoke-duration
              :arena/food
              :arena/poison
              :arena/stone-walls
              :arena/wood-walls
              :arena/zakano
              :arena/perimeter]}]

    (let [current-arena ((get-arena-by-id conn) id)]

      (when-not current-arena
        (db-requirement-error (str "Arena '" id "' does not exist.")))

      (when-not (= (:arena/name current-arena) name)
        (ensure-name-availability conn name))

      (d/transact-async conn [{:db/id (:db/id current-arena)
                               :arena/name name
                               :arena/width width
                               :arena/height height
                               :arena/shot-damage shot-damage
                               :arena/smoke-duraition smoke-duration
                               :arena/food food
                               :arena/poison poison
                               :arena/stone-walls stone-walls
                               :arena/wood-walls wood-walls
                               :arena/zakano zakano
                               :arena/perimeter perimeter}]))))

(defn retract-arena
  "Retracts an arena configuration"
  [conn]
  (fn [arena-id]
    (retract-entity-by-prop conn :arena/id arena-id)))
