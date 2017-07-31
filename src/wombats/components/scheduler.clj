(ns wombats.components.scheduler
  (:require [com.stuartsierra.component :as component]
            [wombats.constants :refer [game-start-time-min
                                       game-start-time-hour
                                       add-game-request]]
            [wombats.scheduler.core :as scheduler]
            [wombats.daos.game :as game]
            [wombats.daos.arena :as arena]
            [wombats.daos.helpers :as helpers]
            [clj-time.core :as t]))

(defrecord Scheduler [config datomic scheduler]
  component/Lifecycle
  (start [component]
    (if scheduler
      component
      (let [conn (get-in datomic [:database :conn])
            aws-credentials (get-in config [:settings :aws])
            lambda-settings (get-in config [:settings :api-settings :lambda])]
        ;; Go through all the pending games, and schedule them
        (assoc component
               :scheduler
               (scheduler/schedule-pending-games (game/get-all-pending-games conn)
                                                 (game/start-game conn aws-credentials lambda-settings))
               :add-game
               (scheduler/automatic-game-scheduler
                {:initial-time (t/today-at game-start-time-hour game-start-time-min)
                 :game-params add-game-request
                 :add-game-fn (game/add-game conn)
                 :gen-id-fn helpers/gen-id
                 :get-game-by-id-fn (game/get-game-by-id conn)
                 :get-arena-by-id-fn (arena/get-arena-by-id conn)
                 :start-game-fn (game/start-game conn aws-credentials lambda-settings)})))))
  (stop [component]
    (if-not scheduler
      component
      (assoc component :scheduler nil))))

;; Public component methods

(defn new-scheduler
  []
  (map->Scheduler {}))
