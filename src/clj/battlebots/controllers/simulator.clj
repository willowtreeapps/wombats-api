(ns battlebots.controllers.simulator
  (:require [ring.util.response :refer [response]]
            [battlebots.game.frame.processor :refer [process-frame]]
            [battlebots.services.github :refer [get-bot]]
            [battlebots.game.utils :as gu]
            [battlebots.game.initializers :refer [initialize-new-round]]
            [battlebots.schemas.simulation :refer [is-simulation?]]))

(defn- end-simulation-round
  "Cleans up a simulated round"
  [{:keys [rounds dirty-arena players] :as game-state}]
  (let [formatted-round {:arena dirty-arena
                         :players (map gu/sanitize-player players)}]
    (merge game-state {:rounds (conj rounds formatted-round)
                       :clean-arena dirty-arena})))

(defn run-simulation
  "Runs a simulated scenario based off user specified parameters"
  [{:keys [arena bot saved-state energy frames] :as simulation} {:keys [_id login]}]
  (is-simulation? simulation)
  (let [player {:_id _id
                :type "player"
                :login login
                :energy energy
                :bot (get-bot _id bot)
                :saved-state saved-state
                :rounds []}
        initial-game-state {:clean-arena arena
                            :players [player]}]
    (loop [game-state initial-game-state
           frame-count frames]
      (if (= frame-count 0)
        (response (select-keys game-state [:rounds]))
        (recur
         ((comp end-simulation-round process-frame initialize-new-round) game-state)
         (dec frame-count))))))
