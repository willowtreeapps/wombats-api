(ns wombats.routes
  (:require [io.pedestal.http :refer [html-body]]
            [io.pedestal.http.body-params :refer [body-params]]
            [io.pedestal.http.ring-middlewares :as ring-middlewares]
            [wombats.interceptors.content-negotiation :refer [coerce-body content-neg-intc]]
            [wombats.interceptors.dao :refer [add-dao-functions]]
            [wombats.interceptors.current-user :refer [add-current-user]]
            [wombats.interceptors.github :refer [add-github-settings]]
            [wombats.interceptors.error-handler :refer [service-error-handler]]
            [wombats.handlers.swagger :as swagger]
            [wombats.handlers.static-pages :as static]
            [wombats.handlers.echo :as echo]
            [wombats.handlers.game :as game]
            [wombats.handlers.user :as user]
            [wombats.handlers.auth :as auth]
            [wombats.handlers.arena :as arena]
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
       ^:interceptors [service-error-handler
                       coerce-body
                       content-neg-intc
                       (body-params)
                       (add-dao-functions (dao/init-dao-map datomic))
                       add-current-user]
       ["/docs" {:get swagger/get-specs}]
       ["/v1"
        ["/self"
         {:get user/get-user-self}]

        ["/users"
         {:get user/get-users}
         ["/:user-id"
          {:get user/get-user-by-id}
          ["/wombats"
           {:get user/get-user-wombats
            :post user/add-user-wombat}
           ["/:wombat-id"
            {:delete user/delete-wombat
             :put user/update-wombat}]]]]

        ["/arenas"
         {:get arena/get-arenas
          :post arena/add-arena}
         ["/:arena-id"
          {:get arena/get-arena-by-id
           :put arena/update-arena
           :delete arena/delete-arena}]]

        ["/games"
         {:get game/get-games
          :post game/add-game}
         ["/:game-id"
          {:get game/get-game-by-id
           :delete game/delete-game}]]

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
