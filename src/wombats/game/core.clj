(ns wombats.game.core
  (:require [wombats.game.initializers :as i]
            [wombats.game.finalizers :as f]
            [wombats.game.player-stats :refer [get-player-stats]]
            [wombats.game.processor :as p]
            [wombats.arena.utils :as au]
            [wombats.sockets.game :as game-sockets]))

(defn- is-end-of-game-type-round?
  [{:keys [game-config frame]}]
  (> (:frame/round-number frame)
     (:game/num-rounds game-config)))

(defn- game-over?
  "End game condition"
  [game-state]
  (case (get-in game-state [:game-config :game/type])
    :round (is-end-of-game-type-round? game-state)))

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
  (clojure.pprint/pprint game-state)

  ;; Print frame number
  #_(prn (get-in game-state [:frame :frame/frame-number]))

  ;; Print number of players
  #_(prn (str "Player Count: " (count (keys (:players game-state)))))

  ;; Sleep before next frame
  (Thread/sleep interval)

  ;; Return game-state
  game-state)

(defn- timeout-frame
  "Pause the frame to allow the client to catch up"
  [game-state time]
  (Thread/sleep time)
  game-state)

(defn- game-loop
  "Game loop"
  [game-state update-frame aws-credentials]
  (loop [current-game-state game-state]
    (if (game-over? current-game-state)
      current-game-state
      (-> current-game-state
          (i/initialize-round)
          (i/initialize-frame)
          (p/source-decisions aws-credentials)
          (p/process-decisions)
          (f/finalize-frame)
          (timeout-frame 500)
          (push-frame-to-clients)
          (push-stats-update-to-clients)
          (push-frame-to-datomic update-frame)
          (f/finalize-round)
          (recur)))))

(defn initialize-game
  "Main entry point for the game engine"
  [game-state
   {update-frame :update-frame
    close-game :close-game}
   aws-credentials]

  (-> game-state
      (i/initialize-game)
      (game-loop update-frame aws-credentials)
      (f/finalize-game)
      (close-game)))
