(ns battlebots.handlers.games
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]
              [battlebots.services.battlebots :refer [get-games
                                                      post-game
                                                      del-game]]))

(defn update-games
  "updates all games in state"
  [db [_ games]]
  (assoc db :games games))

(defn create-game
  "creates a new game"
  [db _]
  (post-game
    #(re-frame/dispatch [:add-game %])
    #(re-frame/dispatch [:update-errors %]))
  db)

(defn add-game
  "adds new game"
  [db [_ game]]
  (assoc db :games (conj (:games db) game)))

(defn set-active-game
  "sets active game"
  [db [_ game]]
  (assoc db :active-game game))

(defn remove-game
  "removes a selected game"
  [db [_ game-id]]
  (del-game game-id
    #(re-frame/dispatch [:filter-game game-id])
    #(re-frame/dispatch [:update-errors %]))
  db)

(defn filter-game
  "filters a game out of state"
  [db [_ game-id]]
  (let [games (:games db)]
    (assoc db :games (remove #(= game-id (:_id %)) games))))

(defn fetch-games
  "fetch all games"
  [db _]
    (get-games
      #(re-frame/dispatch [:update-games %])
      #(re-frame/dispatch [:update-errors %]))
    db)

(re-frame/register-handler :update-games update-games)
(re-frame/register-handler :create-game create-game)
(re-frame/register-handler :add-game add-game)
(re-frame/register-handler :set-active-game set-active-game)
(re-frame/register-handler :remove-game remove-game)
(re-frame/register-handler :filter-game filter-game)
(re-frame/register-handler :fetch-games fetch-games)
