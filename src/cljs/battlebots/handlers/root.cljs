(ns battlebots.handlers.root
    (:require [re-frame.core :as re-frame]

              ;; Handlers
              [battlebots.handlers.account]
              [battlebots.handlers.games]
              [battlebots.handlers.routing]
              [battlebots.handlers.ui]
              [battlebots.handlers.users]

              [battlebots.db :as db]
              [battlebots.services.utils :refer [set-item! get-item]]
              [battlebots.services.battlebots :refer [get-current-user]]
              [cemerick.url :as url]))

(defn initialize-app-state
  "initializes application state on bootstrap"
  [_ _]
  db/default-db)

(defn strip-access-token
  "removes access token from query"
  []
  (let [url (url/url (-> js/window .-location .-href))
        query (:query url)
        location (str (merge url {:query (dissoc query "access-token")}))]
    (set! (.-location js/window) location)))

(defn load-user
  "fetches the current user"
  []
  (get-current-user
   #(re-frame/dispatch [:update-user %])
   #(re-frame/dispatch [:update-errors %])))

(defn bootstrap
  "makes all necessary requests to initially bootstrap an application"
  [db _]
  (let [query (:query (url/url (-> js/window .-location .-href)))
        access-token (get query "access-token")]
    (if access-token
      ;; Access Token was pass by the server. Add token to storage,
      ;; sanitize the URL, and then load user.
      (do
        (set-item! "token" access-token)
        (load-user)
        (strip-access-token))
      ;; User has a token in storage. Load user.
      (if (get-item "token")
        (load-user))))

  (assoc db :bootstrapping? true))

(re-frame/register-handler :initialize-app initialize-app-state)
(re-frame/register-handler :bootstrap-app bootstrap)
