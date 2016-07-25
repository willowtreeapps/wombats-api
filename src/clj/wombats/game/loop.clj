(ns wombats.game.loop
  (:require [wombats.game.initializers :as i]
            [wombats.game.finalizers :as f]
            [wombats.game.frame.processor :as p]))

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
  [initial-game-state config]
  (loop [{:keys [frames] :as game-state} initial-game-state]
    (if-not (game-over? game-state config)
      (let [updated-game-state (-> game-state
                                   i/initialize-frame
                                   (p/process-frame config)
                                   f/finalize-frame)]
        (if (round-over? updated-game-state config)
          (recur (f/finalize-round updated-game-state))
          (recur updated-game-state)))
      game-state)))

(defn start-game
  "Starts the game loop"
  [{:keys [players initial-arena] :as game} config]
  (let [initial-game-state (i/initialize-game game config)
        final-game-state (game-loop initial-game-state config)]
    (f/finalize-game final-game-state)))
