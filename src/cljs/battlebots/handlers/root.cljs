(ns battlebots.handlers.root
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]
              [battlebots.handlers.games]
              [battlebots.handlers.routing]
              [battlebots.handlers.sample]
              [battlebots.handlers.ui]
              [battlebots.services.battlebots :refer [get-games]]))

(defn initialize-app-state
  "initializes application state on bootstrap"
  [_ _]
  db/default-db)

(defn bootstrap
  "makes all necessary requests to initially bootstrap an application"
  [db _]
  (get-games
    #(re-frame/dispatch [:update-games %])
    #(re-frame/dispatch [:update-errors %]))
  (assoc db :bootstrapping? true))

(re-frame/register-handler :initialize-app initialize-app-state)
(re-frame/register-handler :bootstrap-app bootstrap)
