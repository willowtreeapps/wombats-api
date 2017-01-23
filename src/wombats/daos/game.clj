(ns wombats.daos.game
  (:require [datomic.api :as d]
            [wombats.daos.helpers :refer [db-requirement-error
                                          get-entity-by-prop
                                          get-entity-id
                                          get-entities-by-prop
                                          retract-entity-by-prop]]
            [wombats.daos.user :refer [get-user-entity-id
                                       public-user-fields]]))

;; Note about game states:
;;
;; There are four different states that a game can be in.
;; 1. :pending-open   (the game is awaiting players to join and the game has not yet begun)
;; 2. :pending-closed (the game is now longer accepting new player enrollment and the game has not yet begun)
;; 3. :active         (the game is underway)
;; 4. :closed         (the game is finished and archived)
;;
;; Right now these states are hardcoded in their respective DAOs, but this logic should be captured in a FSM
;; like https://github.com/ztellman/automat or even a simple hand rolled one.
;;
;; Looks something like this;
;;
;; 1 → 3 → 4
;; ↓   ↑
;; 2 → ↑

(defn get-all-games
  [conn]
  (fn []
    (get-entities-by-prop conn :game/id)))

(defn get-game-eids-by-status
  [conn]
  (fn [status]
    (let [db (d/db conn)
          formatted-status (if (vector? status)
                             (map keyword status)
                             [(keyword status)])
          game-eids (apply concat
                           (d/q '[:find ?games
                                  :in $ [?status ...]
                                  :where [?games :game/status ?status]]
                                db
                                formatted-status))]
      game-eids)))

(defn get-game-eids-by-player
  [conn]
  (fn [user-id]
    (let [db (d/db conn)
          user-ids (if (vector? user-id)
                     user-id
                     [user-id])
          game-eids (apply concat
                           (d/q '[:find ?games
                                  :in $ [?user-ids ...]
                                  :where [?users :user/id ?user-ids]
                                         [?games :game/players ?users]]
                                db
                                user-ids))]
      game-eids)))

(defn get-games-by-eids
  [conn]
  (fn [game-eids]
    (d/pull-many (d/db conn) '[*] game-eids)))

(defn get-game-by-id
  [conn]
  (fn [game-id]
    (get-entity-by-prop conn :game/id game-id)))

(defn add-game
  "Adds a new game entity to Datomic"
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
