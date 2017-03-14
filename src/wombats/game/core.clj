(ns wombats.game.core
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [wombats.game.initializers :as i]
            [wombats.game.finalizers :as f]
            [wombats.game.processor :as p]
            [wombats.arena.utils :as au]
            [wombats.constants :refer [min-lambda-runtime]]
            [wombats.scheduler.core :as scheduler]
            [wombats.sockets.game :as game-sockets]))

(defn- round-over?
  [{:keys [game-config frame initiative-order]}]
  (let [{round-length :game/round-length} game-config
        {round-start-time :frame/round-start-time} frame
        end-time (t/plus (c/from-date round-start-time)
                         (t/millis round-length))]
    ;; Check to see if all players / ai are dead, or the
    ;; max time limit has elapsed
    (or (empty? initiative-order)
     (t/after? (t/now)
               end-time))))

(defn- push-frame-to-datomic
  [{:keys [frame players] :as game-state} update-frame]
  (update-frame frame players)
  game-state)

(defn- game-loop
  "Game loop"
  [game-state update-frame close-round close-game aws-credentials lambda-settings]
  (loop [current-game-state game-state]
    (let [round-is-over? (round-over? current-game-state)
          current-game-state (cond-> current-game-state
                               ;; Check if the round is over
                               round-is-over?
                               (f/finalize-round close-round))
          current-game-state (-> current-game-state
                                 ;; Check if the game is over
                                 (f/finalize-game close-game))]
      (cond-> current-game-state
        ;; Process the frame if the round isn't over
        (not round-is-over?)
        (-> (p/frame-processor {:aws-credentials aws-credentials
                                :minimum-frame-time min-lambda-runtime
                                :attach-mini-maps false}
                               lambda-settings)
            (game-sockets/broadcast-arena)
            (game-sockets/broadcast-game-info)
            (push-frame-to-datomic update-frame)
            (recur))))))

(defn start-round
  "Main entry point for the game engine"
  [game-state
   {update-frame   :update-frame
    close-round    :close-round
    close-game     :close-game
    round-start-fn :round-start-fn}
   aws-credentials
   lambda-settings]

  (-> game-state
      (i/initialize-round)
      (i/initialize-game-state)
      (game-sockets/broadcast-game-info)
      (game-loop update-frame
                 close-round
                 close-game
                 aws-credentials
                 lambda-settings)
      (game-sockets/broadcast-game-info)
      (game-sockets/broadcast-arena)
      (scheduler/schedule-next-round round-start-fn)))
