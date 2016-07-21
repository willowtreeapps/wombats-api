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

(defn log-shoot-event
  [target-cell damage shooter-id]
  (fn [{:keys [players] :as game-state}]
    ;; TODO This is very basic logging, Shoot will have to be further refactored to support
    ;; fine grain logging of shoot events.
    (if (:energy target-cell)
      (add-messages game-state [{:chan :global
                                 :message (str "Player " shooter-id " shot " (:type target-cell))}])
      game-state)))
