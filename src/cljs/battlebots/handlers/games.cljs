(ns battlebots.handlers.games
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]))

(defn update-games
  "updates all games in state"
  [db games]
  (println games)
  db)

(re-frame/register-handler :update-games update-games)
