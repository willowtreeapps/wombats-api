(ns wombats.game.frame.processor
  (:require [wombats.game.frame.player :refer [resolve-player-turns]]
            [wombats.game.frame.ai :refer [resolve-ai-turns]]))

(defn process-frame
  "Calculates a single frame"
  [game-state]
  (reduce #(%2 %1) game-state [resolve-player-turns
                               resolve-ai-turns]))
