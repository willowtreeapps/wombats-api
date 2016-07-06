(ns battlebots.game.decisions.resolve-shot
  (:require [battlebots.constants.arena :as ac]
            [battlebots.arena.utils :as au]
            [battlebots.game.utils :as gu]))

(defn- resolve-shot-cell
  [{:keys [type] :as cell-at-point} energy]
  (cond
    (< energy 1) cell-at-point
    (ac/can-occupy? type) (assoc-in (:shoot ac/arena-key)
                                    [:md :restore-cell]
                                    cell-at-point)
    (ac/destructible? type) (if (> (get cell-at-point :energy) energy)
                              (assoc cell-at-point
                                :energy
                                (- energy
                                   (get cell-at-point :energy)))
                              (:shoot ac/arena-key))))

(defn- resolve-shoot-damage
  [shoot-coords])

(defn resolve-shoot
  [player-id
   {:keys [direction energy] :as metadata}
   {:keys [dirty-arena] :as game-state}]
  (let [player-coords (gu/get-player-coords player-id dirty-arena)
        shoot-coords (au/draw-line-from-point dirty-arena player-coords direction 10)
        new-dirty-arena (:arena
                         (reduce (fn [{:keys [arena e] :as shoot-state} point]
                                   (let [cell-at-point (au/get-item point arena)
                                         cell-energy (get cell-at-point :energy)
                                         d-key (:display cell-at-point)
                                         updated-cell (resolve-shot-cell
                                                       cell-at-point e)
                                         reduced-e (- e (or cell-energy 0))]
                                     {:arena (au/update-cell
                                              arena point
                                              updated-cell)
                                      :e reduced-e}))
                                 {:arena dirty-arena :e energy}
                                 shoot-coords))]
    (assoc game-state :dirty-arena new-dirty-arena)))
