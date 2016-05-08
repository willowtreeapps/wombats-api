(ns battlebots.controllers.games
  (:require [ring.util.response :refer [response]]
            [battlebots.services.mongodb :refer [get-db]]
            [monger.collection :as mc]))

(defn get-games
  "returns all games or a specified game"
  ([]
   (let [db (get-db)
         games (mc/find-maps db "games")]
     (println games)
     (response [])))
  ([game-id]
    (response {})))

(defn add-game
  "adds a new game"
  []
  (response {}))

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
