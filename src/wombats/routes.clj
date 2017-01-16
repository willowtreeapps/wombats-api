(ns wombats.routes
  (:require [io.pedestal.http :refer [html-body]]
            [wombats.interceptors.content-negotiation :refer [coerce-body content-neg-intc]]
            [wombats.interceptors.dao :refer [add-dao-functions]]
            [wombats.interceptors.github :refer [add-github-settings]]
            [wombats.handlers.static-pages :as static]
            [wombats.handlers.echo :as echo]
            [wombats.handlers.game :as game]
            [wombats.handlers.user :as user]
            [wombats.handlers.auth :as auth]
            [wombats.sockets.chat :as chat-ws]
            [wombats.sockets.game :as game-ws]
            [wombats.daos.core :as dao]))

(defn new-api-router
  [services]
  (let [datomic (get-in services [:datomic :database])
        github (:github services)]
    [[["/"
       ^:interceptors [html-body]
       {:get static/home-page}]
      ["/echo" {:get echo/echo}]
      ["/api"
       ^:interceptors [coerce-body
                       content-neg-intc
                       (add-dao-functions (dao/init-dao-map datomic))]
       ["/v1"
        ["/users"
         {:get user/get-users
          :post user/post-user}
         ["/email/:email"
          {:get user/get-user-by-email}]
         ["/token/:access-token"
          {:get user/get-user-by-access-token}]
         ["/id/:id"
          {:get user/get-user-by-id}]]

        ["/games"
         {:get game/get-games
          :post game/add-game}]

        ["/auth"
         ["/github"
          ^:interceptors [(add-github-settings github)]
          ["/signin"
           {:get auth/github-redirect}]
          ["/callback"
           {:get auth/github-callback}]]]]]]]))

(defn new-ws-router
  [services]
  (let [datomic (get-in services [:datomic :database])
        dao-map (dao/init-dao-map datomic)]
    {"/ws/chat" (chat-ws/chat-room-map dao-map)
     "/ws/game" (game-ws/in-game-ws dao-map)}))
