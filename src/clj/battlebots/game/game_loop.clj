(ns battlebots.game.game-loop
  (:require [battlebots.game.initializers-finalizers :refer [initialize-players
                                                             initialize-game
                                                             initialize-new-round
                                                             finalize-segment
                                                             finalize-round
                                                             finalize-game]]
            [battlebots.constants.game :refer [segment-length
                                               game-length]]
            [battlebots.game.step-operators :refer [resolve-player-turns]]))

(defn- total-rounds
  "Calculates the total number of rounds that have elapsed"
  [num-rounds num-segments]
  (+ (* num-segments segment-length) num-rounds))

(defn- game-loop
  [initial-game-state]
  (loop [{:keys [rounds segment-count] :as game-state} initial-game-state]
    (if (< (total-rounds (count rounds) segment-count) game-length)
      (let [updated-game-state (reduce (fn [game-state update-function]
                                         (update-function game-state))
                                       (initialize-new-round game-state)
                                       [resolve-player-turns])]
        (recur (finalize-round updated-game-state)))
      game-state)))

(defn start-game
  "Starts the game loop"
  [{:keys [players initial-arena] :as game}]
  (let [initial-game-state (initialize-game game)
        final-game-state (game-loop initial-game-state)]
    (finalize-game final-game-state)))
