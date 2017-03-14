(ns wombats.scheduler.core
  (:require [chime :refer [chime-at]]
            [taoensso.timbre :as log]
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
      (chime-at [start-time]
                (fn [time]
                  (start-round-fn game-id))
                {:on-finished
                 (fn []
                   (log/info (str "Starting scheduled round in game " game-id)))
                 :error-handler
                 (fn [e]
                   (log/error (str "Error processing scheduled game " game-id)))}))
    (catch Exception e
      (log/error (str "Error scheduling game " game-id)))))

(defn schedule-next-round
  [game-state round-start-fn]
  (let [{game-status :game/status
         game-id :game/id} (:game-config game-state)
        start-time (get-in game-state [:frame :frame/round-start-time])]
    (when (= game-status :active-intermission)
      (schedule-game game-id start-time round-start-fn)))
  game-state)

(defn schedule-pending-games
  [get-games-fn start-round-fn]
  (let [games (get-games-fn)]
    (doseq [game games]
      (schedule-game (:game/id game)
                     (:game/start-time game)
                     start-round-fn))))
