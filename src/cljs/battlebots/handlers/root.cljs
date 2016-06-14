(ns battlebots.handlers.root
    (:require [re-frame.core :as re-frame]

              ;; Handlers
              [battlebots.handlers.account]
              [battlebots.handlers.games]
              [battlebots.handlers.routing]
              [battlebots.handlers.ui]
              [battlebots.handlers.users]
              [battlebots.socket-handler :refer [initialize-sente-router]]

              [battlebots.db :as db]
              [battlebots.services.utils :refer [set-item! get-item]]
              [battlebots.services.battlebots :refer [get-current-user]]
              [cemerick.url :as url]
              [taoensso.sente :as sente :refer [cb-success?]]))

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

(defn initialize-socket-conneciton
  [db _]
  (let [{:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket! "/chsk" {})
        sente-connection {:chsk chsk
                          :ch-chsk ch-recv
                          :chsk-send! send-fn
                          :chsk-state state}]
    (initialize-sente-router sente-connection)
    (assoc db :socket-connection sente-connection)))

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
(re-frame/register-handler :initialize-socket-connection initialize-socket-conneciton)
