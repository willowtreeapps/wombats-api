(ns battlebots.game.initializers-finalizers
  (:require [battlebots.services.mongodb :as db]
            [battlebots.services.github :refer [get-bot]]
            [battlebots.game.utils :as gu]
            [battlebots.constants.game :refer [segment-length]]))

(defn- save-segment
  [{:keys [_id players rounds segment-count] :as game-state}]
  (db/save-game-segment {:game-id _id
                         :players (map gu/sanitize-player players)
                         :rounds rounds
                         :segment segment-count}))

(defn initialize-players
  "Preps each player map for the game. This player map is different from
  the one that is contained inside of the arena and will contain private data
  including energy, decision logic, and saved state."
  [players]
  (map (fn [{:keys [_id bot-repo] :as player}] (merge player {:energy 100
                                                              :bot (get-bot _id bot-repo)
                                                              :saved-state {}})) players))

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

(defn finalize-segment
  "Batches a segment of rounds together, persists them, and returns a clean segment"
  [{:keys [segment-count] :as game-state}]
  (save-segment game-state)
  (merge game-state {:segment-count (inc segment-count)
                     :rounds []}))

(defn finalize-round
  "Modifies game state to close out a round"
  [{:keys [rounds dirty-arena players] :as game-state}]
  (let [formatted-round {:map dirty-arena
                         :players (map gu/sanitize-player players)}
        updated-game-state (merge game-state {:rounds (conj rounds formatted-round)
                                              :clean-arena dirty-arena})]
    (if (= (count (:rounds updated-game-state)) segment-length)
      (finalize-segment updated-game-state)
      updated-game-state)))

(defn finalize-game
  "Finializes game"
  [{:keys [players] :as game-state}]
  (save-segment game-state)
  (merge (dissoc game-state
                 :clean-arena
                 :dirty-arena
                 :rounds
                 :segment-count) {:state "finalized"
                                  :players (map gu/sanitize-player players)}))
