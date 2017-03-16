(ns wombats.game.finalizers
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [wombats.arena.core :refer [generate-arena]]))

(defn- update-cell-metadata
  [{:keys [meta] :as cell}]
  (let [meta-update (reduce (fn [meta-acc meta-item]
                              (if (or (not (boolean (:decay meta-item)))
                                      (= (:decay meta-item) 0))
                                meta-acc
                                (conj meta-acc
                                      (update meta-item :decay dec))))
                            [] meta)]
    (assoc cell :meta meta-update)))

(defn- update-deterioration
  "Determines the deterioration level and applies it to the element"
  [{:keys [contents] :as cell} max-hp]
  (let [percent-deteriorated (-> (:hp contents)
                                 (/ max-hp)
                                 (float))
        deterioration-key (condp >= percent-deteriorated
                            0.4 :high
                            0.7 :medium
                            :low)]
    (assoc-in cell [:contents :deterioration-level] deterioration-key)))

(defn- update-cell-deterioration-level
  [cell arena-config]
  (case (get-in cell [:contents :type])
    :wombat (update-deterioration cell (:arena/wombat-hp arena-config))
    :zakano (update-deterioration cell (:arena/zakano-hp arena-config))
    :wood-barrier (update-deterioration cell (:arena/wood-wall-hp arena-config))
    :steel-barrier (update-deterioration cell (:arena/steel-wall-hp arena-config))
    cell))

(defn- update-arena-data
  "Maps over each cell updating attributes like metadata decay and damage levels"
  [game-state]
  (let [updated-arena
        (vec (map (fn [row]
                    (vec (map #(-> %
                                   (update-cell-metadata)
                                   (update-cell-deterioration-level (:game/arena game-state))) row)))
                  (get-in game-state [:game/frame :frame/arena])))]
    (assoc-in game-state [:game/frame :frame/arena] updated-arena)))

(defn- round-type-game?
  [game-state]
  (= :high-score (:game/type game-state)))

(defn- add-mini-maps
  "This function is primarily used to support the minimap used
  in the simulator, but could be used in the live mode as well."
  [game-state calculate-decision-maker-state-fn]
  (reduce-kv (fn [game-state player-id player]
               (assoc-in game-state
                         [:game/players player-id :state :mini-map]
                         (vec
                          (map #(vec %)
                               (:arena (calculate-decision-maker-state-fn game-state player-id :wombat))))))
             game-state (:game/players game-state)))

(defn finalize-frame
  [game-state
   {attach-mini-maps :attach-mini-maps}
   calculate-decision-maker-state-fn]
  (cond-> (update-arena-data game-state)
    attach-mini-maps (add-mini-maps calculate-decision-maker-state-fn)))

(defn- format-date
  [date]
  (->> date
       (format "#inst \"%s\"")
       (read-string)))

(defn finalize-round
  [game-state close-round]
  ;; Add intermission to current time
  (let [new-start-time (t/plus (t/now) (t/millis (:game/round-intermission game-state)))
        updated-game-state (-> game-state
                               (assoc-in [:game/frame :frame/round-start-time] (format-date new-start-time))
                               (update-in [:game/frame :frame/round-number] inc)
                               (assoc :game/status :active-intermission)
                               (assoc-in [:game/frame :frame/arena] (generate-arena (:game/arena game-state))))]
    (close-round updated-game-state)
    updated-game-state))

(defn- is-end-of-game-type-round?
  [game-state]
  (> (get-in game-state [:game/frame :frame/round-number])
     (:game/num-rounds game-state)))

(defn- game-over?
  "End game condition"
  [game-state]
  (case (:game/type game-state)
    :high-score (is-end-of-game-type-round? game-state)))

(defn finalize-game
  [game-state close-game]
  (if (game-over? game-state)
    (let [updated-game-state (-> game-state
                                 ;; Decrement the round-number so it stores the last processed round
                                 (update-in [:game/frame :frame/round-number] dec)
                                 (update :game/frame dissoc :frame/round-start-time)
                                 (update :game/frame dissoc :frame/arena)
                                 (assoc :game/end-time (format-date (t/now)))
                                 (assoc :game/status :closed))]

      (close-game updated-game-state)
      updated-game-state)
    game-state))
