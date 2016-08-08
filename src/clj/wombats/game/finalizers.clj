(ns wombats.game.finalizers
  (:require [wombats.services.mongodb :as db]
            [wombats.game.utils :as gu]
            [wombats.constants.arena :as ac]))

(defn- save-round
  [{:keys [_id players frames round-count] :as game-state}]
  (db/save-game-round {:game-id _id
                       :players (map gu/sanitize-player players)
                       :frames frames
                       :round round-count}))

(defn finalize-round
  "Batches a round of frames together, persists them, and returns a clean round"
  [{:keys [round-count] :as game-state}]
  ;; TODO Add persistence back in
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
  ;; (throw (Exception. "Stop Here"))
  (merge (dissoc game-state
                 :clean-arena
                 :dirty-arena
                 :frames
                 :initiative-order
                 :round-count) {:state "finalized"
                                :players (map gu/sanitize-player players)}))
