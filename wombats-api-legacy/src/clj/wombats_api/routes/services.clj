(ns wombats-api.routes.services
  (:require [schema.core :as s]
            [ring.util.http-response :refer :all]
            [ring.swagger.json-schema :as json-schema]
            [compojure.api.sweet :refer :all]
            [wombats-api.schemas.game :as game-schema]
            [wombats-api.controllers.authenication :as auth]
            [wombats-api.controllers.game :as game]
            [wombats-api.controllers.simulator :as simulator]
            [wombats-api.controllers.player :as player]
            monger.json)
   (:import org.bson.types.ObjectId))

(defmethod json-schema/convert-class ObjectId [_ _] {:type "string"})

(defapi service-routes
  {:swagger {:ui "/docs"
             :spec "/swagger.json"
             :data {:info {:version "0.0.1-beta"
                           :title "Wombats API"
                           :description "API for interacting with the wombats game engine / player management system / and arena generation services."}
                    :tags [{:name "games" :description "Games API"}
                           {:name "players" :description "Players API"}
                           {:name "authentication" :description "Authentication API"}
                           {:name "simulator" :description "Simulator API"}]
                    :securityDefinitions {:api_key {:type "apiKey"
                                                    :name "Authorization"
                                                    :in "header"}}}}}

  (context "/api/v1" []
    (context "/games" []
      :tags ["games"]

      (GET "/" []
        :return [game-schema/Game]
        :summary "Returns a collection of games."
        (game/get-games))

      (POST "/" []
        :return game-schema/Game
        :summary "Adds a new game."
        (game/add-game))

      (context "/:game-id" []
        :path-params [game-id :- String]

        (GET "/" []
          :return game-schema/Game
          :summary "Returns a single game."
          (game/get-games game-id))

        (DELETE "/" []
          :return String
          :summary "Removes a game."
          (game/remove-game game-id))

        (POST "/initialize" []
          :return game-schema/Game
          :summary "Initializes a game."
          (game/initialize-game game-id))

        (POST "/start" []
          :return game-schema/Game
          :summary "Starts a game."
          (game/start-game game-id))

        (POST "/join" req
          :body-params [repo :- String]
          :return game-schema/Game
          :summary "Adds the current user to the game."
          (game/add-player game-id (:identity req) repo))

        (context "/frames" []

          (GET "/" []
            :return [game-schema/Frame]
            :summary "Returns all the frames from a game."
            (game/get-game-frames game-id))

          (GET "/:frame-id" []
            :path-params [frame-id :- String]
            :return game-schema/Frame
            :summary "Returns a single frame"
            (ok (game/get-game-frames game-id frame-id))))

        (context "/players" []

          (GET "/" []
            :return [game-schema/InGamePlayer]
            :summary "Returns all the players in a game."
            (ok (game/get-players game-id)))

          (context "/:player-id" []
            :path-params [player-id :- String]

            (GET "/" []
              :return game-schema/InGamePlayer
              :summary "Returns a single player in a game."
              (ok (game/get-players game-id player-id)))))))

    (context "/simulator" []
      :tags ["simulator"]

      (POST "/" req
        :body-params [arena :- game-schema/Arena
                      bot :- String
                      frames :- Integer
                      saved-state :- {}]
        :return [game-schema/SimulationFrame]
        :summary "Runs a game through the simulator."
        (ok (simulator/run-simulation (:params req)
                                      (:identity req)))))

    (context "/players" []
      :tags ["players"]

      (GET "/" []
        :return [game-schema/Player]
        :summary "Return list of players."
        (player/get-players))

      (GET "/profile" req
        :return game-schema/Player
        :summary "Returns the currently logged in user object"
        (player/get-current-player (:identity req)))

      (context "/:player-id" []
        :path-params [player-id :- String]

        (GET "/" []
          :return game-schema/Player
          :summary "Return a player."
          (player/get-players player-id))

        (DELETE "/" []
          :return s/Str
          :summary "Removes a player."
          (player/remove-player player-id))

        (context "/bot" req
          (POST "/" []
            :body-params [repo :- String
                          name :- String]
            :return game-schema/Bot
            :summary "Adds a bot to a player."
            (player/add-player-bot (:identity req) {:repo repo
                                                    :name name}))

          (context "/:repo" []
            :path-params [repo :- String]

            (DELETE "/" req
              :return s/Str
              :summary "Removes a bot from a player."
              (player/remove-player-bot (:identity req) repo))))))

    (context "/signin/github" []
      :tags ["authentication"]

      (GET "/" []
        :summary "Redirects to Github's OAuth2 Service."
        (found (auth/github-signin-route)))

      (GET "/callback" []
        :summary "Process Github's OAuth2 Response"
        :query-params [code :- String
                       state :- String]
        (found (auth/process-user code state))))))
