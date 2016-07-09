(ns battlebots.game.step
  (:require [battlebots.game.step-operators :refer [resolve-player-turns
                                                    resolve-ai-turns]]))

(defn process-step
  "Calculates a single step / frame"
  [game-state]
  (reduce #(%2 %1) game-state [resolve-player-turns
                               resolve-ai-turns]))
