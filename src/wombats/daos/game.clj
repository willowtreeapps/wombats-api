(ns wombats.daos.game
  (:require [datomic.api :as d]
            [wombats.daos.helpers :refer [db-requirement-error
                                          get-entity-by-prop
                                          get-entity-id
                                          get-entities-by-prop
                                          retract-entity-by-prop]]))

(defn get-games
  [conn]
  (fn []
    (get-entities-by-prop conn :game/id)))

(defn get-game-by-id
  [conn]
  (fn [game-id]
    (get-entity-by-prop conn :game/id game-id)))

(defn add-game
  [conn]
  (fn [{:keys [:game/id
              :game/name
              :game/max-players
              :game/type
              :game/is-private
              :game/password
              :game/num-rounds
              :game/round-intermission]} arena-id]
    (let [arena-entity-id (get-entity-id conn :arena/id arena-id)]

      (when-not arena-entity-id
        (db-requirement-error (str "Arena '" arena-id "' not found")))

      (d/transact-async conn [{:db/id (d/tempid :db.part/user)
                               :game/id id
                               :game/name name
                               :game/max-players max-players
                               :game/type type
                               :game/is-private is-private
                               :game/password (or password "")
                               :game/num-rounds (or num-rounds 1)
                               :game/round-intermission (or round-intermission 0)
                               :game/arena arena-entity-id
                               :game/status :open}]))))

(defn retract-game
  [conn]
  (fn [game-id]
    (retract-entity-by-prop conn :game/id game-id)))
