(ns wombats.daos.arena
  (:require [datomic.api :as d]
            [wombats.daos.helpers :refer [gen-id
                                          get-entity-by-prop
                                          get-entity-id]]))

(defn- get-arena-entity-id
  "Returns the entity id of an arena given the public arena id"
  [conn arena-id]
  (get-entity-id conn :arena/id arena-id))

(defn get-arenas
  "Returns a seq of arena configurations"
  [conn]
  (fn []
    (apply concat
           (d/q '[:find (pull ?arena [*])
                  :in $
                  :where [?arena :arena/id]]
                (d/db conn)))))

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

(defn add-arena
  "Sends a new arena configuration to the transactor"
  [conn]
  (fn [{:keys [:arena/name
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
    (d/transact-async conn [{:db/id (d/tempid :db.part/user)
                             :arena/id (gen-id)
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
                             :arena/perimeter perimeter}])))

(defn retract-arena
  "Retracts an arena configuration"
  [conn]
  (fn [arena-id]
    (let [arena-entity-id (get-arena-entity-id conn arena-id)]
      (if arena-entity-id
        (future (do
                  @(d/transact-async conn [[:db.fn/retractEntity arena-entity-id]])
                  (str "Arena " arena-id " removed")))
        (future (str "Arena " arena-id " removed"))))))
