(ns wombats.controllers.simulator
  (:require [ring.util.response :refer [response]]
            [wombats.config.game :as game]
            [wombats.game.frame.processor :refer [process-frame]]
            [wombats.services.github :refer [get-bot]]
            [wombats.game.utils :as gu]
            [wombats.game.initializers :refer [initialize-frame]]
            [wombats.game.finalizers :refer [finalize-frame]]
            [wombats.schemas.simulation :refer [is-simulation?]]))

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
         ((comp finalize-frame #(process-frame % game/config) initialize-frame) game-state)
         (dec frame-count))))))
