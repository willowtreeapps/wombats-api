(ns battlebots.controllers.games
  (:require [ring.util.response :refer [response]]
            [battlebots.services.mongodb :as db]
            [battlebots.arena :as arena]
            [battlebots.game :as game]
            [monger.collection :as mc]
            [monger.result :as mr]
            [monger.operators :refer [$push]])
  (:import org.bson.types.ObjectId))

(def games-coll "games")

(defn get-games
  "returns all games or a specified game"
  ([]
   (response (db/find-all games-coll)))
  ([game-id]
   (response (db/find-one games-coll game-id))))

(defn add-game
  "adds a new game"
  []
  (let [arena (arena/new-arena arena/large-arena)
        game {:initial-arena arena
              :rounds []
              :players []
              :state "pending"}]
    (response (db/insert-one games-coll game))))

(defn initialize-game
  "initializes a game"
  ;; TODO implement FSM to handle game state transitions
  [game-id]
  (let [game (db/find-one games-coll game-id)
        initialized-arena (arena/add-players (:players game) (:initial-arena game))
        updated-game (assoc game :initial-arena initialized-arena :state "initialized")
        update (db/update-one-by-id games-coll game-id updated-game)]
    (if (mr/acknowledged? update)
      (do
        (response updated-game)))))

(defn start-game
  "start game"
  [game-id]
  (let [game (db/find-one games-coll game-id)
        updated-game (assoc game :state "started")
        update (db/update-one-by-id games-coll game-id updated-game)]
    (if (mr/acknowledged? update)
      (do
        (game/start-game updated-game)
        (response updated-game)))))

(defn remove-game
  "removes a game"
  [game-id]
  (db/remove-one games-coll game-id)
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
        game (db/find-one games-coll game-id)
        player-not-registered? (empty? (filter #(= (:_id %) user-id) (:players game)))
        player (select-keys identity [:_id :login])
        update (if player-not-registered?
                 (mc/update (db/get-db) games-coll {:_id (ObjectId. game-id)} {$push {:players player}}))]
    (if player-not-registered?
      (response (db/find-one games-coll game-id))
      (response {:error "user already registered for this game"}))))
