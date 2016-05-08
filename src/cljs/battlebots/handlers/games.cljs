(ns battlebots.handlers.games
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]
              [battlebots.services.battlebots :refer [post-game]]))

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

(re-frame/register-handler :update-games update-games)
(re-frame/register-handler :create-game create-game)
(re-frame/register-handler :add-game add-game)
(re-frame/register-handler :set-active-game set-active-game)
