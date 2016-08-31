(ns wombats-api.game.loop
  (:require [wombats-api.game.initializers :as i]
            [wombats-api.game.finalizers :as f]
            [wombats-api.game.frame.processor :as p]))

(defn- game-over?
  "Determines if a game is over"
  [{:keys [round-count] :as game-state} {:keys [rounds-per-game]}]
  (= round-count rounds-per-game))

(defn- round-over?
  "Determines if a game is over"
  [{:keys [frames] :as game-state} {:keys [frames-per-round]}]
  (= (count frames) frames-per-round))

(defn- game-loop
  "Game loop"
  [initial-game-state]
  (loop [{:keys [frames configuration] :as game-state} initial-game-state]
    (if-not (game-over? game-state configuration)
      (let [updated-game-state (-> game-state
                                   i/initialize-frame
                                   (p/process-frame configuration)
                                   f/finalize-frame)]
        (if (round-over? updated-game-state configuration)
          (recur (f/finalize-round updated-game-state))
          (recur updated-game-state)))
      game-state)))

(defn start-game
  "Starts the game loop"
  [{:keys [players initial-arena configuration] :as game}]
  (let [initial-game-state (i/initialize-game game)
        final-game-state (game-loop initial-game-state)]
    (f/finalize-game final-game-state)))
