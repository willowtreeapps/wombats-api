(ns battlebots.router
  (:require [compojure.core :refer [GET POST DELETE context defroutes]]
            [compojure.route :refer [not-found resources]]
            [battlebots.middleware :refer [wrap-middleware]]
            [battlebots.controllers.games :as games]
            [battlebots.controllers.players :as players]
            [battlebots.views.index :refer [index]]))

(defroutes
  routes

  (context "/games" []
    (GET "/" [] (games/get-games))
    (POST "/" [] (games/add-game))
    (GET "/:game-id" [game-id] (games/get-games game-id))
    (DELETE "/:game-id" [game-id] (games/remove-game game-id))

    (context "/:game-id/rounds" [game-id]
      (GET "/" [] (games/get-rounds game-id))
      (POST "/" [] (games/add-round game-id))
      (GET "/:round-id" [round-id] (games/get-rounds game-id round-id)))

    (context "/:game-id/players" [game-id]
      (GET "/" [] (games/get-players game-id))
      (POST "/" [] (games/add-player game-id))
      (GET "/:player-id" [player-id] (games/get-players game-id player-id))))

  (context "/players" []
    (GET "/" [] (players/get-players))
    (POST "/" req (players/add-player (:params req)))
    (GET "/:player-id" [player-id] (players/get-players player-id))
    (DELETE "/:player-id" [player-id] (players/remove-player player-id)))

  ;; Main view
  (GET "/" [] index)

  ;; Resources servered from this route
  (resources "/")

  ;; No resource found
  (not-found "Not Found")

  (def app (wrap-middleware #'routes)))
