(ns battlebots.game.bot.decisions.move-spec
  (:require [battlebots.game.bot.decisions.move :refer :all :as move]
            [battlebots.arena.utils :refer [get-item update-cell]]
            [battlebots.constants.arena :as ac]
            [battlebots.game.test-game :refer [o b
                                               bot1-private
                                               b1
                                               test-players
                                               test-arena]])
  (:use clojure.test))

(deftest can-occupy-move-spec
  (is (= true (ac/can-occupy? "food" ac/move-settings)) "Bots can occupy food spaces")
  (is (= true (ac/can-occupy? "poison" ac/move-settings)) "Bots can occupy poison spaces")
  (is (= true (ac/can-occupy? "open" ac/move-settings)) "Bots can occupy open spaces")
  (is (= false (ac/can-occupy? "block" ac/move-settings)) "Bots cannot occupy block spaces"))

(deftest apply-collision-damage-spec
  (is (= {:players (assoc-in (assoc-in test-players [0 :energy] 10) [1 :energy] 40)
          :dirty-arena test-arena}
         (#'move/apply-collision-damage "1" [6 3] (get-item [6 4] test-arena) [6 4] 10 {:players test-players
                                                                                        :dirty-arena test-arena}))
      "When a bot collides with another bot, damage is applied to both bots")
  (is (= {:players (assoc-in (assoc-in test-players [0 :energy] -20) [1 :energy] 10)
          :dirty-arena test-arena}
         (#'move/apply-collision-damage "1" [6 3] (get-item [6 4] test-arena) [6 4] 40 {:players test-players
                                                                                        :dirty-arena test-arena}))
      "When a bot collides with another bot, and one or both bots go negitive with their score, neither bot will move, however, both will experience damage.")
  (is (= {:players (assoc-in test-players [0 :energy] -10)
          :dirty-arena (update-cell (update-cell test-arena [5 3] (assoc b1 :energy -10)) [6 3] o)}
         (#'move/apply-collision-damage "1" [6 3] b [5 3] 30 {:players test-players
                                                              :dirty-arena test-arena}))
      "When a bot collides with a wall, damage is applied to both the wall and bot that collided with it. If the wall has zero or less energy after the collision, the bot will take the space of the wall.")
  (is (= {:players (assoc-in test-players [0 :energy] (- (:energy bot1-private) 10))
          :dirty-arena (update-cell (update-cell test-arena
                                                 [6 3]
                                                 (assoc b1 :energy (- (:energy b1) 10)))
                                    [5 3]
                                    (assoc b :energy (- (:energy b) 10)))}
         (#'move/apply-collision-damage "1" [6 3] b [5 3] 10 {:players test-players
                                                              :dirty-arena test-arena}))))
