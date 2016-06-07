(ns battlebots.handlers.ui
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]))

(defn update-errors
  "adds an error to an error queue"
  [db [_ error]]
  (println error)
  db)

(defn display-modal
  "sets a new active modal"
  [db [_ modal]]
  (assoc db :active-modal modal))

(defn display-alert
   "sets a new active alert"
  [db [_ alert-config]]
  (assoc db :active-alert alert-config))

(defn clear-modal
  "clears an active modal"
  [db _]
  (assoc db :active-modal nil))

(defn clear-alert
  "clears an active alert"
  [db _]
  (assoc db :active-alert nil))

(re-frame/register-handler :update-errors update-errors)
(re-frame/register-handler :display-modal display-modal)
(re-frame/register-handler :display-alert display-alert)
(re-frame/register-handler :clear-modal clear-modal)
(re-frame/register-handler :clear-alert clear-alert)
