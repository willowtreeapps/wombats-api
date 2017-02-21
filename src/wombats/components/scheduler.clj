(ns wombats.components.scheduler
  (:require [com.stuartsierra.component :as component]
            [wombats.scheduler.core :as scheduler]
            [wombats.daos.game :as game]))

(defrecord Scheduler [config datomic scheduler]
  component/Lifecycle
  (start [component]
    (if scheduler
      component
      (let [conn (get-in datomic [:database :conn])
            aws-credentials (get-in config [:settings :aws])]
        ;; Go through all the pending games, and schedule them
        (assoc component
               :scheduler
               (scheduler/schedule-pending-games (game/get-all-pending-games conn)
                                                 (game/start-game conn aws-credentials))))))
  (stop [component]
    (if-not scheduler
      component
      (assoc component :scheduler nil))))

;; Public component methods

(defn new-scheduler
  []
  (map->Scheduler {}))
