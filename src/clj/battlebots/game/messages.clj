(ns battlebots.game.messages
  (:require [battlebots.game.utils :as gu]))

(defn- add-player-message
  [player-id players message]
  (gu/modify-player-stats player-id
                          {:messages #(conj % message)}
                          players))

(defn log-collision-event
  "Adds collision events to the frame message system and to each involved
  player's message system"
  [player-id {:keys [type] :as collision-item} damage]
  (fn [{:keys [messages players] :as game-state}]
    (let [updated-frame-messages (conj messages {:type :info
                                                 :message (str "Player " player-id " collided with a " type)
                                                 :metadata {:damage damage}})]
      (if (gu/is-player? collision-item)
        (let [updated-players (reduce (fn [modified-players {:keys [id message]}]
                                        (add-player-message id modified-players message))
                                      players
                                      [{:id player-id
                                        :message {:type :info
                                                  :message (str "You collided with " type "!")
                                                  :metadata {:damage damage}}}
                                       {:id (:_id collision-item)
                                        :message {:type :info
                                                  :message (str "Player " player-id " collided with you!")
                                                  :metadata {:damage damage}}}])]
          (merge game-state {:messages updated-frame-messages
                             :players updated-players}))
        (let [updated-players (add-player-message player-id players {:type :info
                                                                     :message (str "You collided with " type "!")
                                                                     :metadata {:damage damage}})]
          (merge game-state {:messages updated-frame-messages
                             :players updated-players}))))))
