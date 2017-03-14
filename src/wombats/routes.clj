(ns wombats.routes
  (:require [io.pedestal.http :refer [html-body]]
            [io.pedestal.http.body-params :refer [body-params]]
            [io.pedestal.http.ring-middlewares :as ring-middlewares]
            [wombats.interceptors.content-negotiation :refer [coerce-body content-neg-intc]]
            [wombats.interceptors.dao :refer [add-dao-functions]]
            [wombats.interceptors.current-user :refer [add-current-user]]
            [wombats.interceptors.github :refer [add-github-settings]]
            [wombats.interceptors.authorization :refer [add-api-settings
                                                        add-security-settings
                                                        authorize]]
            [wombats.interceptors.error-handler :refer [service-error-handler]]
            [wombats.handlers.swagger :as swagger]
            [wombats.handlers.static-pages :as static]
            [wombats.handlers.echo :as echo]
            [wombats.handlers.game :as game]
            [wombats.handlers.user :as user]
            [wombats.handlers.access-key :as access-key]
            [wombats.handlers.auth :as auth]
            [wombats.handlers.arena :as arena]
            [wombats.handlers.simulator :as simulator]
            [wombats.sockets.game :as game-ws]
            [wombats.daos.core :as dao]))

(defn new-api-router
  [services]
  (let [api-settings (:api-settings services)
        datomic (get-in services [:datomic :database])
        github (:github services)
        security (:security services)
        aws-credentials (:aws services)
        lambda-settings (get-in services [:api-settings :lambda])]
    [[["/"
       ^:interceptors [html-body]
       {:get static/home-page}]
      ["/echo" {:get echo/echo}]
      ["/api"
       ^:interceptors [service-error-handler
                       coerce-body
                       content-neg-intc
                       (body-params)
                       (add-dao-functions (dao/init-dao-map datomic
                                                            aws-credentials
                                                            lambda-settings))
                       add-current-user]
       ["/docs" {:get swagger/get-specs}]
       ["/v1"
        ["/self"
         {:get user/get-user-self}]

        ["/users"
         {:get [:get-users
                user/get-users
                ^:interceptors [(authorize #{:user.roles/admin})]]}
         ["/:user-id"
          {:get [:get-user
                 user/get-user-by-id
                 ^:interceptors [(authorize #{:user.roles/admin})]]}
          ["/wombats"
           ^:interceptors [(authorize #{:user.roles/user})]
           {:get user/get-user-wombats
            :post user/add-user-wombat}
           ["/:wombat-id"
            {:delete user/delete-wombat
             :put user/update-wombat}]]]]

        ["/arenas"
         ^:interceptors [(authorize #{:user.roles/admin})]
         {:get arena/get-arenas
          :post arena/add-arena}
         ["/:arena-id"
          {:get arena/get-arena-by-id
           :put arena/update-arena
           :delete arena/delete-arena}]]

        ["/simulator"
         ["/templates"
          {:get simulator/get-simulator-arena-templates}
          ["/:template-id"
           {:get simulator/get-simulator-arena-template-by-id}]]
         ["/initialize"
          {:post simulator/initialize-simulator}]
         ["/process_frame"
          {:post (simulator/process-simulation-frame aws-credentials
                                                     lambda-settings)}]]

        ["/games"
         {:get [:get-games
                game/get-games
                ^:interceptors [(authorize #{:user.roles/user})]]
          :post [:add-game
                 game/add-game
                 ^:interceptors [(authorize #{:user.roles/admin})]]}
         ["/:game-id"
          ^:interceptors [(authorize #{:user.roles/user})]
          {:get game/get-game-by-id
           :delete game/delete-game}
          ["/join"
           {:put game/join-game}]
          ["/start"
           {:put game/start-game}]]]

        ["/access_keys"
         {:get access-key/get-access-keys
          :post access-key/add-access-key}
         ["/:access-key-id"
          {:get access-key/get-access-key
           :delete access-key/delete-access-key
           :put access-key/update-access-key}]]

        ["/auth"
         ["/github"
          ^:interceptors [(add-api-settings api-settings)
                          (add-security-settings security)
                          (add-github-settings github)]
          ["/signin"
           {:get auth/signin}]
          ["/signout"
           {:get auth/signout}]
          ["/callback"
           {:get auth/github-callback}]]]]]]]))

(defn new-ws-router
  [services]
  (let [datomic (get-in services [:datomic :database])
        aws-credentials (:aws services)
        lambda-settings (get-in services [:api-settings :lambda])
        dao-map (dao/init-dao-map datomic aws-credentials lambda-settings)]
    {"/ws/game" (game-ws/in-game-ws dao-map aws-credentials)}))
