(ns wombats.scheduler.core
  (:require [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn schedule-game
  "Takes in a game, and adds a hook to start the game at start"
  [game-id start-time start-round-fn]
  (try
    (if (t/after? (t/now) (c/from-date start-time))
      ;; Start time has already passed
      (start-round-fn game-id)
      ;; Set interval to start game in the future
      (chime-at [(-> start-time)]
                (fn [time]
                  (start-round-fn game-id))

                ;; TODO: Proper logging
                {:on-finished
                 (fn [] 
                   (prn "Started game."))
                 
                 :error-handler 
                 (fn [e]
                   (prn "Error starting game."))}))
    (catch Exception e
      ;; TODO Add to logger
      (prn e))))

(defn schedule-pending-games
  [get-games-fn start-round-fn]
  (let [games (get-games-fn)]
    (doseq [game games]
      (schedule-game (:game/id game) 
                     (:game/start-time game)
                     start-round-fn))))
