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
                               :game/status :pending-open}]))))

(defn retract-game
  [conn]
  (fn [game-id]
    (retract-entity-by-prop conn :game/id game-id)))

(defn- game-full?
  "Check if the game is full by comparing the currnent number of players to the
  max-players attribute"
  ([game]
   (game-full? game 0))
  ([{:keys [:game/players :game/max-players]} add-n-players]
   (= (+ (count (or players []))
         add-n-players)
      max-players)))

(defn- player-in-game?
  [{:keys [:game/players]} user-eid]
  (contains?
   (->> (or players [])
        (map vals)
        flatten
        set)
   user-eid))

(defn add-player-to-game
  [conn]
  (fn [game-id user-eid]
    (let [game ((get-game-by-id conn) game-id)]

      ;; Check for game existence
      (when-not game
        (db-requirement-error (str "Game '" game-id "' was not found.")))

      ;; Check to see if the game is accepting new players
      (when-not (= (:game/status game) :pending-open)
        (db-requirement-error (str "Game '" game-id "' is not accepting new players")))

      ;; Check to see if the player is already in the game
      (when (player-in-game? game user-eid)
        (db-requirement-error (str "User '" user-eid "' is already in game '" game-id "'.")))

      ;; This next part builds up the transaction(s)
      ;; 1. Add the join transaction
      ;; 2. If the game is now full, add the :pending closed transaction
      (let [trx (cond-> []
                  true (conj {:db/id (:db/id game)
                              :game/players user-eid})

                  (game-full? game 1) (conj {:db/id (:db/id game)
                                             :game/status :pending-closed}))]
        (d/transact-async conn trx)))))
