(ns battlebots.game.bot.decisions.save-state)

(defn save-state
  "Player state is player described. This means players can choose to save any key
  value data they choose. The state will persist and will be sent back to the player
  each turn. A play can update state as much as they would like"
  [player-id metadata {:keys [players] :as game-state}]
  (let [updated-players (map #(if (= (:_id %) player-id)
                                (assoc % :saved-state metadata)
                                %) players)]
    (assoc game-state :players updated-players)))
