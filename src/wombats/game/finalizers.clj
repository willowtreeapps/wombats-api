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
  [{:keys [arena-config] :as game-state}]
  (let [updated-arena
        (vec (map (fn [row]
                    (vec (map #(-> %
                                   (update-cell-metadata)
                                   (update-cell-deterioration-level arena-config)) row)))
                  (get-in game-state [:frame :frame/arena])))]
    (assoc-in game-state [:frame :frame/arena] updated-arena)))

(defn- round-type-game?
  [game-state]
  (= :high-score (get-in game-state [:game-config :game/type])))

(defn finalize-frame
  [game-state]
  (-> game-state
      (update-arena-data)))

(defn- format-date
  [date]
  (->> date
       (format "#inst \"%s\"")
       (read-string)))

(defn finalize-round
  [game-state close-round]
  ;; Add intermission to current time
  (let [intermission (get-in game-state [:game-config :game/round-intermission])
        new-start-time (t/plus (t/now) (t/millis intermission))
        updated-game-state (-> game-state
                               (assoc-in [:frame :frame/round-start-time] (format-date new-start-time))
                               (update-in [:frame :frame/round-number] inc)
                               (assoc-in [:game-config :game/status] :active-intermission)
                               (assoc-in [:frame :frame/arena] (generate-arena (:arena-config game-state))))]
    (close-round updated-game-state)
    updated-game-state))

(defn- is-end-of-game-type-round?
  [{:keys [game-config frame]}]
  (> (:frame/round-number frame)
     (:game/num-rounds game-config)))

(defn- game-over?
  "End game condition"
  [game-state]
  (case (get-in game-state [:game-config :game/type])
    :high-score (is-end-of-game-type-round? game-state)))

(defn finalize-game
  [game-state close-game]
  (if (game-over? game-state)
    (let [updated-game-state (-> game-state
                                 ;; Decrement the round-number so it stores the last processed round
                                 (update-in [:frame :frame/round-number] dec)
                                 (update :frame dissoc :frame/round-start-time)
                                 (update :frame dissoc :frame/arena)
                                 (assoc-in [:game-config :game/end-time] (format-date (t/now)))
                                 (assoc-in [:game-config :game/status] :closed))]

      (close-game updated-game-state)
      updated-game-state)
    game-state))
