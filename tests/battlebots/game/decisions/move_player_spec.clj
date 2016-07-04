(ns battlebots.game.decisions.move-player-spec
  (:require [battlebots.game.decisions.move-player :refer :all :as move-player]
            [battlebots.arena.utils :refer [get-item update-cell]]
            [battlebots.game.test-game :refer [o b
                                               bot1-private
                                               b1
                                               test-players
                                               test-arena]])
  (:use clojure.test))

(deftest can-occupy-space-spec
  (is (= true (#'move-player/can-occupy-space? {:type "food"})) "Bots can occupy food spaces")
  (is (= true (#'move-player/can-occupy-space? {:type "poison"})) "Bots can occupy poison spaces")
  (is (= true (#'move-player/can-occupy-space? {:type "open"})) "Bots can occupy open spaces")
  (is (= false (#'move-player/can-occupy-space? {:type "block"})) "Bots cannot occupy block spaces"))

(deftest apply-collision-damage-spec
  (is (= {:players (assoc-in (assoc-in test-players [0 :energy] 10) [1 :energy] 40)
          :dirty-arena test-arena}
         ((#'move-player/apply-collision-damage "1" [6 3] (get-item [6 4] test-arena) [6 4] 10) {:players test-players
                                                                                          :dirty-arena test-arena}))
      "When a bot collides with another bot, damage is applied to both bots")
  (is (= {:players (assoc-in (assoc-in test-players [0 :energy] -20) [1 :energy] 10)
          :dirty-arena test-arena}
         ((#'move-player/apply-collision-damage "1" [6 3] (get-item [6 4] test-arena) [6 4] 40) {:players test-players
                                                                                          :dirty-arena test-arena}))
      "When a bot collides with another bot, and one or both bots go negitive with their score, neither bot will move, however, both will experience damage.")
  (is (= {:players (assoc-in test-players [0 :energy] -10)
          :dirty-arena (update-cell (update-cell test-arena [5 3] (assoc b1 :energy -10)) [6 3] o)}
         ((#'move-player/apply-collision-damage "1" [6 3] b [5 3] 30) {:players test-players
                                                                :dirty-arena test-arena}))
      "When a bot collides with a wall, damage is applied to both the wall and bot that collided with it. If the wall has zero or less energy after the collision, the bot will take the space of the wall.")
  (is (= {:players (assoc-in test-players [0 :energy] (- (:energy bot1-private) 10))
          :dirty-arena (update-cell (update-cell test-arena
                                                 [6 3]
                                                 (assoc b1 :energy (- (:energy b1) 10)))
                                    [5 3]
                                    (assoc b :energy (- (:energy b) 10)))}
         ((#'move-player/apply-collision-damage "1" [6 3] b [5 3] 10) {:players test-players
                                                                :dirty-arena test-arena}))))
