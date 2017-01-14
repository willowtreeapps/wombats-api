(ns wombats-api.game.bot.decisions.smokescreen
  (:require [wombats-api.arena.utils :as au]
            [wombats-api.game.utils :as gu]))

(defn- surrounding-coords
  "Return a collection of coords that should be smokescreened given a central starting point"
  [coords arena-dims]
  (conj (map #(au/adjust-coords coords % arena-dims) (range 8)) coords))

(defn- process-smokescreen
  "Update a cell in an arena with smokescreen"
  [uuid duration arena point]
  (let [cell (au/get-item point arena)]
    (au/update-cell arena point
                    (assoc-in cell [:md (keyword uuid)] {:type :smokescreen
                                                         :decay duration}))))

(defn smokescreen
  "Main smokescreen function"
  [{:keys [dirty-arena] :as game-state}
   {:keys [smokescreen-duration] :as config}
   {:keys [uuid decision-maker-coords]}]
  (let [arena-dims (au/get-arena-dimensions dirty-arena)
        smoke-coords (surrounding-coords decision-maker-coords arena-dims)
        smokescreen-uuid (au/uuid)]
    (assoc game-state :dirty-arena (reduce
                                    #(process-smokescreen smokescreen-uuid
                                                          smokescreen-duration
                                                          %1
                                                          %2)
                                    dirty-arena
                                    smoke-coords))))
