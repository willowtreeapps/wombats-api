(ns battlebots.game
  (:require [battlebots.arena :as arena]
            [battlebots.sample-bots.bot-one :as bot-one]))

(def test-players [{:_id 1 :login "AI1"}
                   {:_id 2 :login "AI2"}
                   {:_id 3 :login "AI2"}])

(defn apply-turn
  "Applies players turn to the current state of the game"
  [{:keys [arena players] :as game-state} player-id]
  (let [player (first (filter #(= (:_id %) player-id) players))]
    ;; TODO Apply turn and modify game-state
    game-state))

(defn randomize-players
  "Randomizes player ids"
  [players]
  (shuffle (map #(:_id %) players)))

(defn resolve-player-turns
  "Updates the arena by applying each players movement logic"
  [{:keys [players] :as game-state}]
  (let [execution-order (randomize-players players)]
    (reduce apply-turn game-state execution-order)))

(defn initialize-players
  "Preps each player map for the game"
  [players]
  ;; TODO remove when done testing
  (concat players test-players))

(defn start-game
  "Starts the game loop"
  [{:keys [players] :as game}]
  ;; TODO Start Game Here
  ;;
  ;; Notes: Waiting on bot registration logic for each player,
  ;; however game logic could still be developed by creating a
  ;; function that is applied to all bots. This will allow
  ;; the build out of game logic without applying user specific
  ;; logic

  (loop [round 0
         game game
         players (initialize-players players)]
    (let [arena (or (:current-arena game) (:initial-arena game))]
      (when (< round 50)
        (let [update (reduce (fn [game-state update-function]
                               (update-function game-state)) {:arena arena
                                                              :players players} [resolve-player-turns])]
          (recur (+ round 1)
                 game
                 players))))))
