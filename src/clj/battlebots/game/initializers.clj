(ns battlebots.game.initializers
  (:require [battlebots.services.github :refer [get-bot]]))

(defn initialize-players
  "Preps each player map for the game. This player map is different from
  the one that is contained inside of the arena and will contain private data
  including energy, decision logic, and saved state."
  [players]
  (map (fn [{:keys [_id bot-repo] :as player}] (merge player {:energy 100
                                                              :bot (get-bot _id bot-repo)
                                                              :saved-state {}
                                                              :type "player"})) players))

(defn initialize-game
  "Preps the game"
  [{:keys [initial-arena players] :as game}]
  (merge game {:clean-arena initial-arena
               :rounds []
               :segment-count 0
               :players (initialize-players players)}))

(defn initialize-new-round
  "Preps game-state for a new round"
  [{:keys [clean-arena] :as game-state}]
  (merge game-state {:dirty-arena clean-arena}))
