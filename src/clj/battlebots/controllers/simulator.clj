(ns battlebots.controllers.simulator
  (:require [ring.util.response :refer [response]]
            [battlebots.game.frame.processor :refer [process-frame]]
            [battlebots.services.github :refer [get-bot]]
            [battlebots.game.utils :as gu]
            [battlebots.game.initializers :refer [initialize-frame]]
            [battlebots.schemas.simulation :refer [is-simulation?]]))

(defn- end-simulation-frame
  "Cleans up a simulated frame"
  [{:keys [frames dirty-arena players] :as game-state}]
  (let [formatted-frame {:arena dirty-arena
                         :players (map gu/sanitize-player players)}]
    (merge game-state {:frames (conj frames formatted-frame)
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
                :frames []}
        initial-game-state {:clean-arena arena
                            :players [player]}]
    (loop [game-state initial-game-state
           frame-count frames]
      (if (= frame-count 0)
        (response (select-keys game-state [:frames]))
        (recur
         ((comp end-simulation-frame process-frame initialize-frame) game-state)
         (dec frame-count))))))
