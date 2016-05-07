(ns battlebots.services.battlebots
  (:require [ajax.core :refer [GET POST]]))

(defn get-games
  "fetches a list of all available games"
  [on-success on-error]
  (GET "/games" {:handler on-success
                 :error-handler on-error}))
