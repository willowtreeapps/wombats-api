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
  [player-id {:keys [type] :as collision-item} damage]
  (fn [game-state]
    (let [messages [{:chan :global
                     :message (str "Player " player-id " collided with a " type)}
                    {:chan player-id
                     :message (str "You collided with " type "!")}]]
      (if (gu/is-player? collision-item)
        (add-messages game-state (conj messages {:chan (:_id collision-item)
                                                 :message (str "Player " player-id " collided with you!")}))
        (add-messages game-state messages)))))
