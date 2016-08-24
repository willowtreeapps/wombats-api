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
   (db/get-all-games))
  ([game-id]
   (db/get-game game-id)))

(defn add-game
  []
  (let [game {:initial-arena (generate/new-arena small-arena)
              :players []
              :state "pending"}]
    (db/add-game game)))

(defn remove-game
  [game-id]
  (let [update (db/remove-game game-id)]
    (when (mr/acknowledged? update)
      (str "Game " game-id " removed."))))

(defn initialize-game
  ;; TODO implement FSM to handle game state transitions
  [game-id]
  (let [game (db/get-game game-id)
        initialized-arena (generate/add-players (:players game) (:initial-arena game))
        updated-game (assoc game :initial-arena initialized-arena :state "initialized")
        update (db/update-game game-id updated-game)]
    (when (mr/acknowledged? update)
      updated-game)))

(defn start-game
  [game-id]
  (let [game (db/get-game game-id)
        ;; updated-game (assoc game :state "started")
        updated-game (game-loop/start-game game config)
        update (db/update-game game-id updated-game)]
    (when (mr/acknowledged? update)
      (dissoc updated-game :messages))))

(defn get-game-frames
  ([game-id]
    (db/get-game-frames game-id))
  ([game-id frame-id]
    (db/get-game-frames game-id frame-id)))

(defn add-player
  [game-id {:keys [_id login] :as identity} repo]
  (let [game (db/get-game game-id)
        player-not-registered? (empty? (filter #(= (:_id %) _id) (:players game)))
        player {:_id (str _id)
                :login login
                :bot-repo repo}
        update (if player-not-registered?
                 (db/add-player-to-game game-id player))]
    (if player-not-registered?
      (db/get-game game-id)
      (bad-request! "You are already registered in this game."))))

(defn get-players
  ([game-id]
   (:players (get-games game-id)))
  ([game-id player-id]
   (first (filter #(= player-id (:_id %))
                  (:players (get-games game-id))))))

;; (defn get-round
;;   "Returns a game round"
;;   [game-id round-number]
;;   (response (db/get-game-round game-id round-number)))

;; (defn add-frame
;;   "adds a new frame to a given game"
;;   [game-id]
;;     (response {}))
