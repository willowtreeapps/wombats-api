(ns battlebots.game.decisions.resolve-shot
  (:require [battlebots.constants.arena :as ac]
            [battlebots.arena.utils :as au]
            [battlebots.game.utils :as gu]))

(defn- add-shot-metadata
  [cell]
  ;; TODO This will change per the metadata discussion
  ;; Keeping the current implementation for the time being
  (assoc-in (:shoot ac/arena-key) [:md :restore-cell] cell))

(defn- add-shot-damage
  "Add damage to cells that contain the energy prop"
  [damage]
  (fn [cell]
    (if (:energy cell)
      (assoc cell :energy (- (:energy cell) damage))
      cell)))

(defn resolve-shot-cell
  [{:keys [type] :as cell-at-point} damage]
  (reduce (fn [cell update-func]
            (update-func cell)) cell-at-point [(add-shot-damage damage)
                                               add-shot-metadata]))

(defn- shot-should-progress?
  "Returns a boolean indicating if a shot should continue down it's path"
  [should-progress? cell-at-point energy]
  (boolean (and should-progress?
               (> energy 0)
               (ac/can-occupy? (:type cell-at-point) ac/shot-settings))))

(defn- process-shot
  "Process a cell that a shot passes through"
  [{:keys [arena energy should-progress?] :as shoot-state} point]
  (let [cell-at-point (au/get-item point arena)]
    (if (shot-should-progress? should-progress? cell-at-point energy)
      (let [cell-energy (get cell-at-point :energy)
            reduced-energy (Math/max 0 (- energy (or cell-energy 0)))
            energy-delta (- energy reduced-energy)
            updated-cell (resolve-shot-cell cell-at-point energy-delta)]
        {:arena (au/update-cell arena point updated-cell)
         :energy reduced-energy
         :should-progress? true})
      (assoc shoot-state :should-progress? false))))

(defn resolve-shoot
  "Main shoot function"
  [player-id
   {:keys [direction energy] :as metadata}
   {:keys [dirty-arena] :as game-state}]
  (let [player-coords (gu/get-player-coords player-id dirty-arena)
        shoot-coords (au/draw-line-from-point dirty-arena player-coords direction 10)
        new-dirty-arena (:arena (reduce process-shot {:arena dirty-arena
                                                      :energy energy
                                                      :should-progress? true} shoot-coords))]
    (assoc game-state :dirty-arena new-dirty-arena)))
