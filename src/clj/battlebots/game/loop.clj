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

(defn- game-over?
  "Determines if a game is over"
  [{:keys [frames segment-count] :as game-state}]
  (>= (total-frames (count frames) segment-count) game-length))

(defn- round-over?
  "Determines if a game is over"
  [{:keys [frames] :as game-state}]
  (= (count frames) segment-length))

(defn- game-loop
  "Game loop"
  [initial-game-state]
  (loop [{:keys [frames segment-count] :as game-state} initial-game-state]
    (if-not (game-over? game-state)
      (let [updated-game-state ((comp finalize-frame process-frame initialize-frame) game-state)]
        (if (round-over? updated-game-state)
          (recur (finalize-segment updated-game-state))
          (recur updated-game-state)))
      game-state)))

(defn start-game
  "Starts the game loop"
  [{:keys [players initial-arena] :as game}]
  (let [initial-game-state (initialize-game game)
        final-game-state (game-loop initial-game-state)]
    (finalize-game final-game-state)))
