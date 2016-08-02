(ns wombats.game.frame.processor
  (:require [wombats.game.frame.initiative :as initiative]
            [wombats.game.frame.turns :as turns]))

(defn process-frame
  "Calculates a single frame"
  [game-state config]
  (-> game-state
      (initiative/update-initiative-order)
      (turns/resolve-turns config)))
