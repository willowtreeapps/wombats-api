(ns wombats.handlers.root
    (:require [re-frame.core :as re-frame]

              ;; Handlers
              [wombats.handlers.account]
              [wombats.handlers.games]
              [wombats.handlers.routing]
              [wombats.handlers.ui]
              [wombats.handlers.users]
              [wombats.socket-handler :refer [initialize-sente-router]]

              [wombats.db :as db]
              [wombats.services.utils :refer [set-item! get-item]]
              [wombats.services.wombats :refer [get-current-user]]
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

(defn initialize-socket-connection
  [db _]
  (let [{:keys [chsk
                ch-recv
                send-fn
                state]} (sente/make-channel-socket! "/chsk" {:type :auto
                                                                    :packer :edn
                                                                    :params {:access-token (get-item "token")}
                                                                    :wrap-recv-envs false})
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
        (strip-access-token))
      ;; User has a token in storage. Load user.
      (if (get-item "token")
        (do
          (load-user)
          (re-frame/dispatch [:initialize-socket-connection])))))

  (assoc db :bootstrapping? true))

(re-frame/register-handler :initialize-app initialize-app-state)
(re-frame/register-handler :bootstrap-app bootstrap)
(re-frame/register-handler :initialize-socket-connection initialize-socket-connection)
