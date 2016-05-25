(ns battlebots.controllers.players
  (:require [ring.util.response :refer [response]]
            [battlebots.services.mongodb :as db]))

(def players-coll "players")

(defn get-players
  "returns all players or a specified player"
  ([]
   (response (db/find-all players-coll)))
  ([player-id]
   (response (db/find-one players-coll player-id))))

(defn remove-player
  "removes a specified player"
  [player-id]
  (db/remove-one players-coll player-id)
  (response "ok"))
