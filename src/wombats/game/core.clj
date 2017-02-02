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
  #_(au/print-arena (:frame/arena frame))

  ;; Pretty print the full arena state
  #_(clojure.pprint/pprint (:frame/arena frame))

  ;; Pretty print everything but the arena
  (clojure.pprint/pprint
   (dissoc game-state :frame))

  ;; Pretty print everything
  #_(clojure.pprint/pprint game-state)

  ;; Sleep before next frame
  (Thread/sleep interval)

  ;; Return game-state
  game-state)

(defn- game-loop
  "Game loop"
  [game-state aws-credentials]
  (loop [current-game-state game-state]
    (if (game-over? current-game-state)
      current-game-state
      (-> current-game-state
          (i/initialize-frame)
          (p/source-decisions aws-credentials)
          (p/process-decisions)
          (f/finalize-frame)
          #_(frame-debugger 1000)
          (recur)))))

(defn initialize-game
  "Main entry point for the game engine"
  [game-state aws-credentials]
  (-> game-state
      (i/initialize-game)
      (game-loop aws-credentials)
      #_(frame-debugger 0)
      (f/finalize-game)))
