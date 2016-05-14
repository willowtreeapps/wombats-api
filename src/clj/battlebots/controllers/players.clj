(ns battlebots.controllers.players
  (:require [ring.util.response :refer [response]]))

(defn get-players
  "returns all players or a specified player"
  ([]
   (response []))
  ([player-id]
   (response {})))

(defn add-player
  "adds a player"
  []
  (response {}))

(defn remove-player
  "removes a specified player"
  [player-id]
  (response "ok"))
