(ns wombats-api.game.finalizers
  (:require [wombats-api.db.core :as db]
            [wombats-api.game.utils :as gu]
            [wombats-api.constants.arena :as ac]))

(defn- save-round
  [{:keys [_id frames round-count] :as game-state}]
  (when (> (count frames) 0)
    (db/save-game-round (map
                         #(merge % {:game-id _id
                                    :round-number round-count})
                         frames))))

(defn finalize-round
  "Batches a round of frames together, persists them, and returns a clean round"
  [{:keys [round-count] :as game-state}]
  (save-round game-state)
  (merge game-state {:round-count (inc round-count)
                     :frames []}))

(defn finalize-frame
  "Modifies game state to close out a frame"
  [{:keys [frames dirty-arena players messages] :as game-state}]
  (let [formatted-frame {:map dirty-arena
                         :players (map gu/sanitize-player players)
                         :messages messages}]
    (merge game-state {:frames (conj frames formatted-frame)
                       :clean-arena dirty-arena})))

(defn finalize-game
  "Finializes game"
  [{:keys [players] :as game-state}]
  (save-round game-state)
  (merge (dissoc game-state
                 :clean-arena
                 :dirty-arena
                 :frames
                 :initiative-order
                 :messages
                 :round-count) {:state "finalized"
                                :players (map gu/sanitize-player players)}))
