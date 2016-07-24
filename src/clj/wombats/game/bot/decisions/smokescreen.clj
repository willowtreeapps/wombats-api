(ns wombats.game.bot.decisions.smokescreen
  (:require [wombats.arena.utils :as au]
            [wombats.game.utils :as gu]))

(defn- surrounding-coords
  "Return a collection of coords that should be smokescreened given a central starting point"
  [coords arena-dims]
  (conj (map #(au/adjust-coords coords % arena-dims) (range 8)) coords))

(defn- process-smokescreen
  "Update a cell in an arena with smokescreen"
  [uuid arena point]
  (let [cell (au/get-item point arena)]
    (au/update-cell arena point
                    (assoc-in cell [:md (keyword uuid)] {:type :smokescreen
                                                         :decay 5}))))

(defn smokescreen
  "Main smokescreen function"
  [player-id {:keys [dirty-arena] :as game-state}]
  (let [player-coords (gu/get-player-coords player-id dirty-arena)
        arena-dims (au/get-arena-dimensions dirty-arena)
        smoke-coords (surrounding-coords player-coords arena-dims)
        smokescreen-uuid (au/uuid)]
    (assoc game-state :dirty-arena (reduce
                                    #(process-smokescreen smokescreen-uuid %1 %2)
                                    dirty-arena
                                    smoke-coords))))
