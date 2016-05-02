(ns battlebots.handlers
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]))

(re-frame/register-handler
  :initialize-db
  (fn  [_ _]
    db/default-db))

(re-frame/register-handler
  :set-active-panel
  (fn [db [_ active-panel]]
    (assoc db :active-panel active-panel)))

(re-frame/register-handler
  :update-word
  (fn [db [_ word]]
    (assoc db :word word)))
