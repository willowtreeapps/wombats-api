(ns wombats-api.controllers.simulator
  (:require [wombats-api.config.game :as game]
            [wombats-api.game.frame.processor :refer [process-frame]]
            [wombats-api.services.github :refer [get-bot]]
            [wombats-api.game.utils :as gu]
            [wombats-api.game.initializers :refer [initialize-frame]]
            [wombats-api.game.finalizers :refer [finalize-frame]]))

(defn run-simulation
  "Runs a simulated scenario based off user specified parameters"
  [{:keys [arena bot saved-state frames] :as simulation} {:keys [_id login]}]
  (let [player {:_id _id
                :type "player"
                :login login
                :bot (get-bot _id bot)
                :saved-state saved-state
                :frames []}
        initial-game-state {:clean-arena arena
                            :players [player]}]
    (loop [game-state initial-game-state
           frame-count frames]
      (if (= frame-count 0)
        (vec (:frames game-state))
        (recur
         ((comp finalize-frame
                #(process-frame % game/config)
                initialize-frame) game-state)
         (dec frame-count))))))
