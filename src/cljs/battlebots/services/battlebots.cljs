(ns battlebots.services.battlebots
  (:require [ajax.core :refer [GET POST]]))

(defn get-games
  "fetches a list of all available games"
  [on-success on-error]
  (GET "/games" {:response-format :json
                 :keywords? true
                 :handler on-success
                 :error-handler on-error}))

(defn post-game
  "creates a new game record"
  [on-success on-error]
  (POST "/games" {:response-format :json
                 :keywords? true
                 :handler on-success
                 :error-handler on-error}))
