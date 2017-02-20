(ns wombats.scheduler.core
  (:require [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn- start-game
  "Starts the game"
  []
  (prn "STARTING GAME"))

(defn schedule-game
  "Takes in a game, and adds a hook to start the game at start"
  [game]
  (let [start-time (c/from-date (:game/start-time game))]

    (if (t/after? (t/now) start-time)
      ;; Start time has already passed
      (start-game)
      ;; Set interval to start game in the future
      (chime-at [(-> start-time)]
                (fn [time]
                  (start-game))))

    nil))
