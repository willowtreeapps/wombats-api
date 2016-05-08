(ns battlebots.controllers.games
  (:require [ring.util.response :refer [response]]
            [battlebots.services.mongodb :refer [get-db]]
            [monger.collection :as mc]
            [battlebots.arena :as arena])
  (:import org.bson.types.ObjectId))

(def games-coll "games")

(defn get-games
  "returns all games or a specified game"
  ([]
    (let [db (get-db)
          games (mc/find-maps db games-coll)]
      (response games)))
  ([game-id]
    (let [db (get-db)
          game (mc/find-one-as-map db games-coll {:_id (ObjectId. game-id)})]
      (response game))))

(defn add-game
  "adds a new game"
  []
  (let [db (get-db)
        _id (ObjectId.)
        arena (arena/new-arena arena/large-arena)
        game {:_id _id
              :initial-arena arena
              :rounds []
              :players []}]
    (mc/insert db games-coll game)
    (response game)))

(defn remove-game
  "removes a game"
  [game-id]
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
  "add a new player to a given game"
  [game-id]
  (response {}))
