(ns battlebots.controllers.games
  (:require [ring.util.response :refer [response]]
            [battlebots.services.mongodb :as db]
            [battlebots.arena :as arena]
            [battlebots.game :as game]
            [monger.result :as mr])
  (:import org.bson.types.ObjectId))

;; TODO Remove when done testing
(def test-players [{:_id 1 :login "AI1"}
                   {:_id 2 :login "AI2"}
                   {:_id 3 :login "AI2"}])

(defn get-games
  "returns all games or a specified game"
  ([]
   (response (db/get-all-games)))
  ([game-id]
   (response (db/get-game game-id))))

(defn add-game
  "adds a new game"
  []
  (let [arena (arena/new-arena arena/large-arena)
        game {:initial-arena arena
              :players test-players
              :state "pending"}]
    (response (db/add-game game))))

(defn initialize-game
  "initializes a game"
  ;; TODO implement FSM to handle game state transitions
  [game-id]
  (let [game (db/get-game game-id)
        initialized-arena (arena/add-players (:players game) (:initial-arena game))
        updated-game (assoc game :initial-arena initialized-arena :state "initialized")
        update (db/update-game game-id updated-game)]
    (if (mr/acknowledged? update)
      (response updated-game))))

(defn start-game
  "start game"
  [game-id]
  (let [game (db/get-game game-id)
        ;; updated-game (assoc game :state "started")
        updated-game (game/start-game game)
        update (db/update-game game-id updated-game)]
    (if (mr/acknowledged? update)
      (response updated-game))))

(defn remove-game
  "removes a game"
  [game-id]
  (db/remove-game game-id)
  (response "ok"))

(defn get-rounds
  "returns all rounds, or a specifed round, for a given game"
  ([game-id]
    (response []))
  ([game-id round-id]
    (response {})))

(defn add-round
  "adds a new round to a given game"
  [game-id]
    (response {}))

(defn get-players
  "returns all players, or a specified player, for a given game"
  ([game-id]
    (response []))
  ([game-id player-id]
    (response {})))

(defn add-player
  "add a new player to a given game

  TODO: find-and-modify would prevent an additional database query"
  [game-id identity]
  (let [user-id (:_id identity)
        game (db/get-game game-id)
        player-not-registered? (empty? (filter #(= (:_id %) user-id) (:players game)))
        player (select-keys identity [:_id :login])
        update (if player-not-registered?
                 (db/add-player-to-game game-id player))]
    (if player-not-registered?
      (response (db/get-game game-id))
      (response {:error "user already registered for this game"}))))
