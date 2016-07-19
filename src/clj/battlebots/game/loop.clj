(ns battlebots.game.loop
  (:require [battlebots.game.initializers :refer [initialize-players
                                                  initialize-game
                                                  initialize-frame]]
            [battlebots.game.finalizers :refer [finalize-segment
                                                finalize-frame
                                                finalize-game]]
            [battlebots.constants.game :refer [segment-length
                                               game-length]]
            [battlebots.game.frame.processor :refer [process-frame]]))

(defn- total-frames
  "Calculates the total number of frames that have elapsed"
  [num-frames num-segments]
  (+ (* num-segments segment-length) num-frames))

(defn- game-loop
  "Game loop"
  [initial-game-state]
  (loop [{:keys [frames segment-count] :as game-state} initial-game-state]
    (if (< (total-frames (count frames) segment-count) game-length)
      (let [updated-game-state (process-frame (initialize-frame game-state))]
        (recur (finalize-frame updated-game-state)))
      game-state)))

(defn start-game
  "Starts the game loop"
  [{:keys [players initial-arena] :as game}]
  (let [initial-game-state (initialize-game game)
        final-game-state (game-loop initial-game-state)]
    (finalize-game final-game-state)))
