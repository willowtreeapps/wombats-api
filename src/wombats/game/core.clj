(ns wombats.game.core
  (:require [clj-time.coerce :as c] 
            [clj-time.core :as t]
            [wombats.game.initializers :as i]
            [wombats.game.finalizers :as f]
            [wombats.game.player-stats :refer [get-player-stats]]
            [wombats.game.processor :as p]
            [wombats.arena.utils :as au]
            [wombats.scheduler.core :as scheduler]
            [wombats.sockets.game :as game-sockets]))

(defn- round-over?
  [{:keys [game-config frame]}]
  (let [{round-length :game/round-length} game-config
        {round-start-time :frame/round-start-time} frame
        end-time (t/plus (c/from-date round-start-time)
                         (t/millis round-length))]
    (t/after? (t/now)
              end-time)))

(defn- push-frame-to-clients
  [{:keys [frame] :as game-state}]

  (game-sockets/broadcast-arena
   (:game-id game-state)
   (:frame/arena frame))

  game-state)

(defn- push-frame-to-datomic
  [{:keys [frame players] :as game-state} update-frame]
  (update-frame frame players)
  game-state)

(defn- push-stats-update-to-clients
  [game-state]
  (game-sockets/broadcast-stats
    (:game-id game-state)
    (get-player-stats game-state))
  game-state)

(defn frame-debugger
  "This is a debugger that will print out a ton of additional
  information in between each frame"
  [{:keys [frame] :as game-state} interval]

  ;; Pretty print the arena
  #_(au/print-arena (:frame/arena frame))

  ;; Pretty print the full arena state
  #_(clojure.pprint/pprint (:frame/arena frame))

  ;; Pretty print everything but the arena
  #_(clojure.pprint/pprint
   (dissoc game-state :frame))

  ;; Pretty print everything
  #_(clojure.pprint/pprint game-state)

  ;; Print frame number
  (prn (format "Round Number: %d" 
               (get-in game-state [:frame :frame/round-number])))

  ;; Print frame number
  (prn (format "Frame Number: %d" 
               (get-in game-state [:frame :frame/frame-number])))

  ;; Print number of players
  #_(prn (str "Player Count: " (count (keys (:players game-state)))))

  ;; Print game status
  (prn (format "Game Status: %s" 
               (get-in game-state [:game-config :game/status])))

  ;; Sleep before next frame
  (Thread/sleep interval)

  ;; Return game-state
  game-state)

(defn- timeout-frame
  "Pause the frame to allow the client to catch up"
  [game-state time]
  (Thread/sleep time)
  game-state)

(defn- frame-processor
  [game-state update-frame aws-credentials]
  (-> game-state
      (i/initialize-frame)
      (p/source-decisions aws-credentials)
      (p/process-decisions)
      (f/finalize-frame)
      ;; TODO: Remove this timeout
      #_(timeout-frame 500)
      (push-frame-to-clients)
      (push-stats-update-to-clients)
      (push-frame-to-datomic update-frame)))

(defn- game-loop
  "Game loop"
  [game-state update-frame close-round close-game aws-credentials]
  (loop [current-game-state game-state]
    (let [round-is-over? (round-over? current-game-state)]       

      (cond-> current-game-state
        ;; Check if the round is over
        round-is-over?
        (f/finalize-round close-round)

        ;; Check if the game is over
        true
        (f/finalize-game close-game)

        ;; Process the frame if the round isn't over
        (not round-is-over?)
        (frame-processor update-frame aws-credentials)
        
        ;; Recur if the round isn't over
        (not round-is-over?)
        (recur)))))

(defn- schedule-next-round
  [game-state round-start-fn]
  (let [{game-status :game/status
         game-id :game/id} (:game-config game-state)
        start-time (get-in game-state [:frame :frame/round-start-time])]
    (when (= game-status :active-intermission)
      (scheduler/schedule-game game-id start-time round-start-fn)))
  (clojure.pprint/pprint (get-in game-state [:game-config]))
  game-state)

(defn initialize-round
  "Main entry point for the game engine"
  [game-state
   {update-frame   :update-frame
    close-round    :close-round
    close-game     :close-game
    round-start-fn :round-start-fn}
   aws-credentials]

  (-> game-state
      (i/initialize-round)
      (game-loop update-frame
                 close-round
                 close-game
                 aws-credentials)
      (schedule-next-round round-start-fn)
      (frame-debugger 0)))
