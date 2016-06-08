(ns battlebots.game
  (:require [battlebots.arena :as arena]))

(defn add-players
  "place players around the arena and return a new arean"
  [players arena]
  (reduce arena/replacer arena players))

(defn start-game
  "Starts the game loop"
  [{:keys [initial-arena players] :as game}]
  ;; TODO Start Game Here
  ;;
  ;; Notes: Waiting on bot registration logic for each player,
  ;; however game logic could still be developed by creating a
  ;; function that is applied to all bots. This will allow
  ;; the build out of game logic without applying user specific
  ;; logic
  )
