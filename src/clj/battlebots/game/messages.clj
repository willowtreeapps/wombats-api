(ns battlebots.game.messages
  (:require [battlebots.game.utils :as gu]))

(defn- add-messages
  [game-state new-messages]
  (reduce (fn [game-state-memo {:keys [chan message]}]
            (let [channel (keyword chan)
                  update (conj (get-in game-state-memo [:messages channel] []) message)]
              (assoc-in game-state-memo [:messages channel] update)))
          game-state
          new-messages))

(defn log-collision-event
  "Adds collision events to the frame message system and to each involved
  player's message system"
  [player-id {:keys [type] :as collision-item} damage game-state]
  (let [messages [{:chan :global
                   :message (str "Player " player-id " collided with a " type)}
                  {:chan player-id
                   :message (str "You collided with " type "!")}]]
    (if (gu/is-player? collision-item)
      (add-messages game-state (conj messages {:chan (:_id collision-item)
                                               :message (str "Player " player-id " collided with you!")}))
      (add-messages game-state messages))))

(defn log-shoot-event
  [game-state target-cell damage shooter-id]
  (add-messages game-state [{:chan :global
                             :message (str "Player " shooter-id " shot " (:type target-cell))}
                            {:chan shooter-id
                             :message (str "You shot " (:type target-cell) "!")}]))

(defn log-victim-shot-event
  [game-state victim-id shooter-id damage]
  (add-messages game-state [{:chan victim-id
                             :message (str "You were shot by " shooter-id "! You took " damage " damage.")}]))
