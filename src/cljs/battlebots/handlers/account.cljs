(ns battlebots.handlers.account
  (:require [re-frame.core :as re-frame]
            [battlebots.services.battlebots :refer [get-current-user post-credentials post-new-credentials]]
            [battlebots.services.utils :refer [set-item! remove-item!]]))

(defn update-user
  "updates the user object"
  [db [_ user]]
  (assoc db :user user))

(defn update-auth-token
  "updates a users auth token"
  [db [_ auth-token]]
  (let [token (:token auth-token)]
    (set-item! "token" token)
    
    ;; Fetch newly logged in user object
    (re-frame/dispatch [:get-user])
    
    ;; Redirect Home
    (re-frame/dispatch [:set-active-panel :home-panel])
    (set! (-> js/window .-location .-hash) "#/")

    ;; Update token
    (assoc db :auth-token token)))

(defn sign-in
  "signs a user in"
  [db [_ user-credentials]]
  (post-credentials
    user-credentials
    #(re-frame/dispatch [:update-auth-token %])
    #(re-frame/dispatch [:update-errors %]))
  db)

(defn sign-up
  "creates a new user account"
  [db [_ user-credentials]]
  (post-new-credentials
   user-credentials
   #(re-frame/dispatch [:update-auth-token %])
   #(re-frame/dispatch [:udate-errors %]))
  db)

(defn sign-out
  "signs a user out"
  [db [_ _]]
  (remove-item! "token")
  (assoc db :auth-token nil :user nil))

(defn get-user
  "fetches logged in user object"
  [db [_ user]]
  (get-current-user
    #(re-frame/dispatch [:update-user %])
    #(re-frame/dispatch [:update-errors %]))
  db)

(re-frame/register-handler :update-user update-user)
(re-frame/register-handler :update-auth-token update-auth-token)

(re-frame/register-handler :sign-in sign-in)
(re-frame/register-handler :sign-up sign-up)
(re-frame/register-handler :sign-out sign-out)
(re-frame/register-handler :get-user get-user)
