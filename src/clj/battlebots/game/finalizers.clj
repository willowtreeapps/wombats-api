(ns battlebots.game.finalizers
  (:require [battlebots.services.mongodb :as db]
            [battlebots.game.utils :as gu]
            [battlebots.constants.arena :as ac]))

(defn- save-segment
  [{:keys [_id players frames segment-count] :as game-state}]
  (db/save-game-segment {:game-id _id
                         :players (map gu/sanitize-player players)
                         :frames frames
                         :segment segment-count}))

(defn finalize-segment
  "Batches a segment of frames together, persists them, and returns a clean segment"
  [{:keys [segment-count] :as game-state}]
  (save-segment game-state)
  (merge game-state {:segment-count (inc segment-count)
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
  (save-segment game-state)
  (merge (dissoc game-state
                 :clean-arena
                 :dirty-arena
                 :frames
                 :segment-count) {:state "finalized"
                                  :players (map gu/sanitize-player players)}))
