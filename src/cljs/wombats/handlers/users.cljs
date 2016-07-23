(ns wombats.handlers.users
    (:require [re-frame.core :as re-frame]
              [wombats.db :as db]
              [wombats.services.wombats :refer [del-user get-users]]))

(defn update-users
  "Updates users for CMS"
  [db [_ users]]
  (assoc db :users users))

(defn fetch-users
  "Admin call to fetch all users"
  [db _]
  (get-users
    #(re-frame/dispatch [:update-users %])
    #(re-frame/dispatch [:update-errors %]))
  db)

(defn filter-user
  "Filters user out of memory"
  [db [_ user-id]]
  (let [users (:users db)]
    (assoc db :users (remove #(= user-id (:_id %)) users))))

(defn remove-user
  "Admin call to remove a user"
  [db [_ user-id]]
  (del-user user-id
    #(re-frame/dispatch [:filter-user user-id])
    #(re-frame/dispatch [:update-errors %]))
  db)

(re-frame/register-handler :fetch-users fetch-users)
(re-frame/register-handler :update-users update-users)
(re-frame/register-handler :remove-user remove-user)
(re-frame/register-handler :filter-user filter-user)
