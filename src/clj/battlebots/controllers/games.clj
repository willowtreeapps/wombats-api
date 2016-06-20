(ns battlebots.controllers.games
  (:require [ring.util.response :refer [response]]
            [battlebots.services.mongodb :as db]
            [battlebots.constants.arena :refer [small-arena large-arena]]
            [battlebots.arena :as arena]
            [battlebots.game :as game]
            [monger.result :as mr])
  (:import org.bson.types.ObjectId))

;; TODO Remove when done testing
(def test-players [{:_id 1 :login "AI1"}
                   {:_id 2 :login "AI2"}
                   {:_id 3 :login "AI3"}
                   {:_id 4 :login "AI4"}
                   {:_id 5 :login "AI5"}
                   {:_id 6 :login "AI6"}
                   {:_id 7 :login "AI7"}
                   {:_id 8 :login "AI8"}
                   {:_id 9 :login "AI9"}
                   {:_id 10 :login "AI10"}
                   {:_id 11 :login "AI11"}
                   {:_id 12 :login "AI12"}])

(defn get-games
  "returns all games or a specified game"
  ([]
   (response (db/get-all-games)))
  ([game-id]
   (response (db/get-game game-id))))

(defn add-game
  "adds a new game"
  []
  (let [arena (arena/new-arena large-arena)
        game {:initial-arena arena
              :players test-players
              :state "pending"}]
    (response (db/add-game game))))

(defn initialize-game
  "initializes a game"
  ;; TODO implement FSM to handle game state transitions
  [game-id]
  (let [game (db/get-game game-id)
        initialized-arena (arena/add-players (:players game) (:initial-arena game))
        updated-game (assoc game :initial-arena initialized-arena :state "initialized")
        update (db/update-game game-id updated-game)]
    (if (mr/acknowledged? update)
      (response updated-game))))

(defn start-game
  "start game"
  [game-id]
  (let [game (db/get-game game-id)
        ;; updated-game (assoc game :state "started")
        updated-game (game/start-game game)
        update (db/update-game game-id updated-game)]
    (if (mr/acknowledged? update)
      (response updated-game))))

(defn remove-game
  "removes a game"
  [game-id]
  (db/remove-game game-id)
  (response "ok"))

(defn get-rounds
  "returns all rounds, or a specifed round, for a given game"
  ([game-id]
    (response []))
  ([game-id round-id]
    (response {})))

(defn add-round
  "adds a new round to a given game"
  [game-id]
    (response {}))

(defn get-players
  "returns all players, or a specified player, for a given game"
  ([game-id]
    (response []))
  ([game-id player-id]
    (response {})))

(defn add-player
  "add a new player to a given game

  TODO: find-and-modify would prevent an additional database query"
  [game-id player-id bot]
  (let [{:keys [_id login]} (db/get-player player-id)
        game (db/get-game game-id)
        player-not-registered? (empty? (filter #(= (:_id %) player-id) (:players game)))
        player {:_id (str _id)
                :login login
                :bot-repo (:repo bot)}
        update (if player-not-registered?
                 (db/add-player-to-game game-id player))]
    (if player-not-registered?
      (response (db/get-game game-id))
      (response {:error "user already registered for this game"}))))
