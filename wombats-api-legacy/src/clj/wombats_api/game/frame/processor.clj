(ns wombats-api.game.frame.processor
  (:require [wombats-api.game.frame.initiative :as initiative]
            [wombats-api.game.frame.turns :as turns]))

(defn process-frame
  "Calculates a single frame"
  [game-state config]
  (-> game-state
      (initiative/update-initiative-order)
      (turns/resolve-turns config)))
