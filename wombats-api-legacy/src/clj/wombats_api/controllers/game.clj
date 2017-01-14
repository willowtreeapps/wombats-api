(ns wombats-api.controllers.game
  (:require [monger.result :as mr]
            [ring.util.http-response :refer :all]
            [wombats-api.db.core :as db]
            [wombats-api.arena.generation :as generate]
            [wombats-api.constants.arena :refer [small-arena large-arena]]
            [wombats-api.game.loop :as game-loop]
            [wombats-api.config.game :refer [config]]))

(defn get-games
  ([]
   (ok (db/get-all-games)))
  ([game-id]
   (ok (db/get-game game-id))))

(defn add-game
  []
  (let [game {:initial-arena (generate/new-arena small-arena)
              :configuration config
              :players []
              :state "pending"}]
    (ok (db/add-game game))))

(defn remove-game
  [game-id]
  (let [update (db/remove-game game-id)]
    (when (mr/acknowledged? update)
      (ok (str "Game " game-id " removed.")))))

(defn initialize-game
  ;; TODO implement FSM to handle game state transitions
  [game-id]
  (let [game (db/get-game game-id)
        initialized-arena (generate/add-players (:players game) (:initial-arena game))
        updated-game (assoc game :initial-arena initialized-arena :state "initialized")
        update (db/update-game game-id updated-game)]
    (when (mr/acknowledged? update)
      (ok updated-game))))

(defn start-game
  [game-id]
  (let [game (db/get-game game-id)
        updated-game (game-loop/start-game game)
        update (db/update-game game-id updated-game)]
    (when (mr/acknowledged? update)
      (ok (dissoc updated-game :messages)))))

(defn get-game-frames
  ([game-id]
    (db/get-game-frames game-id))
  ([game-id frame-id]
    (db/get-game-frames game-id frame-id)))

(defn add-player
  [game-id {:keys [_id login] :as identity} repo]
  (let [{:keys [configuration players] :as game} (db/get-game game-id)
        {:keys [max-players]} configuration
        game-open? (< (count players) max-players)
        player-registered? (not (empty? (filter #(= (:_id %) _id) players)))
        player {:_id (str _id)
                :login login
                :bot-repo repo}]
    ;; Add player if criteria is met
    (when (and (not player-registered?) game-open?)
      (db/add-player-to-game game-id player))
    ;; Response
    (cond
      (not game-open?) (bad-request! "The game you are trying to join is now full.")
      player-registered? (bad-request! "You are already registered in this game.")
      :else (ok (db/get-game game-id)))))

(defn get-players
  ([game-id]
   (:players (get-games game-id)))
  ([game-id player-id]
   (first (filter #(= player-id (:_id %))
                  (:players (get-games game-id))))))
