(ns battlebots.handlers.ui
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]))

(defn update-errors
  "adds an error to an error queue"
  [db error]
  (println error)
  db)

(re-frame/register-handler :update-errors update-errors)
