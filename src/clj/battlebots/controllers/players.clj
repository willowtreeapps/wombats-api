(ns battlebots.controllers.players
  (:require [ring.util.response :refer [response]]
            [battlebots.services.mongodb :as db]
            [battlebots.schemas.player :refer [isPlayer]])
  (:import org.bson.types.ObjectId))

(def players-coll "players")

(defn get-players
  "returns all players or a specified player"
  ([]
   (response (db/find-all players-coll)))
  ([player-id]
   (response (db/find-one players-coll player-id))))

(defn add-player
  "adds a player record to the db"
  [player]
  (let [player-object (isPlayer player)]
    (response (db/insert-one players-coll player-object))))

(defn remove-player
  "removes a specified player"
  [player-id]
  (db/remove-one players-coll player-id)
  (response "ok"))
