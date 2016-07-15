(ns battlebots.game.finalizers
  (:require [battlebots.services.mongodb :as db]
            [battlebots.game.utils :as gu]
            [battlebots.constants.arena :as ac]
            [battlebots.constants.game :refer [segment-length]]))

(defn- save-segment
  [{:keys [_id players rounds segment-count] :as game-state}]
  (db/save-game-segment {:game-id _id
                         :players (map gu/sanitize-player players)
                         :rounds rounds
                         :segment segment-count}))

(defn- update-cell-metadata
  [{:keys [md] :as cell}]
  (assoc cell :md (into {} (map (fn [md-entry]
                                  (let [md-uuid (first md-entry)
                                        md-value (last md-entry)
                                        md-update (assoc md-value :decay (dec (:decay md-value)))]
                                    (if (> 0 (:decay md-update)) nil [md-uuid md-update]))) md))))

(defn update-volatile-cells
  "currently reverts all volatile cells to open if decay-turns is less than 1"
  [arena]
  (vec (map (fn [row]
              (vec (map #(update-cell-metadata %) row))) arena)))

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
                                              :clean-arena (update-volatile-cells dirty-arena)})]
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
