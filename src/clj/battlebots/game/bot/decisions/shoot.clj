(ns battlebots.game.bot.decisions.shoot
  (:require [battlebots.constants.arena :as ac]
            [battlebots.arena.utils :as au]
            [battlebots.game.utils :as gu]))

(defn- add-shot-metadata
  [uuid]
  (fn [cell]
    (assoc-in cell [:md (keyword uuid)] {:type :shot
                                         :decay 1})))

(defn- add-shot-damage
  "Add damage to cells that contain the energy prop"
  [damage]
  (fn [cell]
    (if (:energy cell)
      (assoc cell :energy (- (:energy cell) damage))
      cell)))

(defn- replace-destroyed-cell
  [shot-uuid]
  (fn [cell]
    (let [destructible? (ac/destructible? (:type cell) ac/shot-settings)
          destroyed? (if destructible?
                       (<= (:energy cell) 0)
                       false)
          updated-md (when destroyed?
                       (assoc (:md cell) (keyword shot-uuid) {:type :destroyed
                                                              :decay 1}))]
      (if destroyed?
        (assoc (:open ac/arena-key) :md updated-md)
        cell))))

(defn- resolve-shot-cell
  "Aggregation pipeline for resolving what should happen to a cell when a shot enters it's space"
  [cell-at-point damage shot-uuid]
  (reduce (fn [cell update-func]
            (update-func cell)) cell-at-point [(add-shot-damage damage)
                                               (add-shot-metadata shot-uuid)
                                               (replace-destroyed-cell shot-uuid)]))

(defn- shot-should-progress?
  "Returns a boolean indicating if a shot should continue down it's path"
  [should-progress? cell-at-point energy]
  (boolean (and should-progress?
               (> energy 0)
               (ac/can-occupy? (:type cell-at-point) ac/shot-settings))))

(defn- update-victim-energy
  [{:keys [players shooter-id cell damage] :as player-shot-update}]
  (if (gu/is-player? cell)
    (assoc player-shot-update :players (gu/modify-player-stats
                                        (:_id cell)
                                        {:energy #(- % damage)}
                                        players))
    player-shot-update))

(defn- reward-shooter
  "Shooters get rewarded for hitting a cell with energy. How much depends on the cell type."
  [{:keys [players shooter-id cell damage] :as player-shot-update}]
  (let [updated-players
        (condp = (:type cell)
          "player" (gu/modify-player-stats shooter-id {:energy #(+ % (* 2 damage))} players)
          "ai"     (gu/modify-player-stats shooter-id {:energy #(+ % (* 2 damage))} players)
          "block"  (gu/modify-player-stats shooter-id {:energy #(+ % damage)} players)
          players)]
    (assoc player-shot-update :players updated-players)))

(defn- process-shot
  "Process a cell that a shot passes through"
  [{:keys [game-state energy should-progress?
           shot-uuid shooter-id] :as shoot-state} point]
  (let [{:keys [dirty-arena players]} game-state
        cell-at-point (au/get-item point dirty-arena)]
    (if (shot-should-progress? should-progress? cell-at-point energy)
      (let [cell-energy (:energy cell-at-point)
            remaining-energy (Math/max 0 (- energy (or cell-energy 0)))
            energy-delta (- energy remaining-energy)
            updated-cell (resolve-shot-cell cell-at-point energy-delta shot-uuid)
            updated-dirty-arena (au/update-cell dirty-arena point updated-cell)
            updated-players (:players (reduce (fn [players update-fn]
                                                (update-fn players))
                                              {:players players
                                               :shooter-id shooter-id
                                               :cell cell-at-point
                                               :damage energy-delta}
                                              [reward-shooter
                                               update-victim-energy]))]
        {:game-state (merge game-state {:dirty-arena updated-dirty-arena
                                        :players updated-players})
         :energy remaining-energy
         :should-progress? true
         :shot-uuid shot-uuid
         :shooter-id shooter-id})
      (assoc shoot-state :should-progress? false))))

(defn shoot
  "Main shoot function"
  [player-id
   {:keys [direction energy] :as metadata}
   {:keys [dirty-arena players] :as game-state}]
  (let [player-coords (gu/get-player-coords player-id dirty-arena)
        shoot-coords (au/draw-line-from-point dirty-arena
                                              player-coords
                                              direction
                                              (:distance ac/shot-settings))
        players-update-shooter-energy (gu/modify-player-stats
                                       player-id
                                       {:energy #(- % energy)}
                                       players)]
    (:game-state (reduce
                  process-shot
                  {:game-state (assoc game-state
                                 :players players-update-shooter-energy)
                   :energy energy
                   :should-progress? true
                   :shot-uuid (au/uuid)
                   :shooter-id player-id} shoot-coords))))
