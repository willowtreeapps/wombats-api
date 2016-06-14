(ns battlebots.controllers.players
  (:require [ring.util.response :refer [response]]
            [battlebots.services.mongodb :as db]))

(defn get-players
  "returns all players or a specified player"
  ([]
   (response (db/get-all-players)))
  ([player-id]
   (response (db/get-player player-id))))

(defn remove-player
  "removes a specified player"
  [player-id]
  (db/remove-player player-id)
  (response "ok"))
