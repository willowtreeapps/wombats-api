(ns wombats.game.finalizers
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]))

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
  (= :round (get-in game-state [:game-config :game/type])))

(defn- end-of-round?
  [{:keys [game-config frame] :as game-state}]
  (let [{round-length :game/round-length} game-config
        {round-start-time :frame/round-start-time} frame
        end-time (t/plus (c/from-date (read-string round-start-time))
                         (t/millis round-length))]
    (t/after? (t/now)
              end-time)))

(defn finalize-frame
  [game-state]
  (-> game-state
      (update-arena-data)))

(defn finalize-round
  [game-state]
  (if (and (round-type-game? game-state)
           (end-of-round? game-state))
    (assoc-in game-state [:frame :frame/round-start-time] nil)
    game-state))

(defn finalize-game
  [game-state]
  game-state)
