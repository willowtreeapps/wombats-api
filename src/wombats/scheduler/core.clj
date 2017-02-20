(ns wombats.scheduler.core
  (:require [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn schedule-game
  "Takes in a game, and adds a hook to start the game at start"
  [game start-game-fn]
  (let [start-time (c/from-date (:game/start-time game))]

    (if (t/after? (t/now) start-time)
      ;; Start time has already passed
      (start-game-fn game)
      ;; Set interval to start game in the future
      (chime-at [(-> start-time)]
                (fn [time]
                  (start-game-fn game))

                ;; TODO: Proper logging
                {:on-finished
                 (fn [] 
                   (prn "Started game."))
                 
                 :error-handler 
                 (fn [e]
                   (prn "Error starting game."))}))))

(defn schedule-pending-games
  [get-games-fn start-game-fn]
  (let [games (get-games-fn)]
    (doseq [game games]
      (schedule-game game start-game-fn))))
