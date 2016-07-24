(ns wombats.game.frame.processor
  (:require [wombats.game.frame.player :as player]))

(defn process-frame
  "Calculates a single frame"
  [game-state config]
  (-> game-state
      (player/resolve-turns config)))
