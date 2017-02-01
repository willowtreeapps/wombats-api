(ns wombats.game.core
  (:require [wombats.game.initializers :as i]
            [wombats.game.finalizers :as f]
            [wombats.game.processor :as p]
            [wombats.arena.utils :as au]))

(defn- game-over?
  "End game condition"
  [game-state]
  ;; TODO For now we're only calculating a fix number of rounds
  ;; This will have to be updated with the base condition for
  ;; each game type
  (= 10 (get-in game-state [:frame :frame/frame-number])))

(defn- frame-debugger
  "This is a debugger that will print out a ton of additional
  information in between each frame"
  [{:keys [frame] :as game-state} interval]

  ;; Pretty print the arena
  (au/print-arena (:frame/arena frame))

  ;; Sleep before next frame
  (Thread/sleep interval)

  ;; Return game-state
  game-state)

(defn- game-loop
  "Game loop"
  [game-state]
  (loop [current-game-state game-state]
    (if (game-over? current-game-state)
      current-game-state
      (-> current-game-state
          (i/initialize-frame)
          (p/source-decisions)
          (p/process-decisions)
          #_(frame-debugger 1000)
          (f/finalize-frame)
          (recur)))))

(defn initialize-game
  "Main entry point for the game engine"
  [game-state]
  (-> game-state
      (i/initialize-game)
      (game-loop)
      (f/finalize-game)))
