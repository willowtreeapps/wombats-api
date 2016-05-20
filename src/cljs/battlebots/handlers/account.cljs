(ns battlebots.handlers.account
  (:require [re-frame.core :as re-frame]
            [battlebots.db :refer [account auth-token]]))

(defn update-current-user
  "updates the logged in user object"
  [db [_ current-user]]
  db)

(defn update-auth-token
  "updates a users auth token"
  [db [_ auth-token]]
  (swap! auth-token merge {:token auth-token})
  db)

(defn sign-in
  "signs a user in"
  [db [_ user-data]]
  (.log js/console (str user-data)))

(re-frame/register-handler :update-current-user update-current-user)
(re-frame/register-handler :sign-in sign-in)
