(ns battlebots.controllers.playground
  (:require [ring.util.response :refer [response]]
            [battlebots.game.step :refer [process-step]]
            [battlebots.services.github :refer [get-bot]]
            [battlebots.game.utils :as gu]
            [battlebots.game.initializers-finalizers :refer [initialize-new-round
                                                             update-volatile-positions]]
            [battlebots.schemas.simulation :refer [is-simulation?]]))

(defn- end-simulation-round
  "Cleans up a simulated round"
  [{:keys [rounds dirty-arena players] :as game-state}]
  (let [formatted-round {:arena dirty-arena
                         :players (map gu/sanitize-player players)}]
    (merge game-state {:rounds (conj rounds formatted-round)
                       :clean-arena (update-volatile-positions dirty-arena)})))

(defn run-simulation
  "Runs a simulated scenario based off user specified parameters"
  [{:keys [arena bot saved-state energy steps] :as simulation} {:keys [_id login]}]
  (is-simulation? simulation)
  (let [player {:_id _id
                :login login
                :energy energy
                :bot (get-bot _id bot)
                :saved-state saved-state
                :rounds []}
        initial-game-state {:clean-arena arena
                            :players [player]}]
    (loop [game-state initial-game-state
           step-count steps]
      (if (= step-count 0)
        (response (select-keys game-state [:rounds]))
        (recur
         ((comp end-simulation-round process-step initialize-new-round) game-state)
         (dec step-count))))))
