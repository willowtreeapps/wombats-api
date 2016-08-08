(ns wombats.game.bot.decisions.move-spec
  (:require [wombats.game.bot.decisions.move :refer :all :as move]
            [wombats.arena.utils :refer [get-item update-cell]]
            [wombats.game.frame.turns :as turns]
            [wombats.constants.arena :as ac]
            [wombats.game.test-game :refer [o b
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
  (is (= {:players (-> test-players
                       (assoc-in [0 :hp] 10)
                       (assoc-in [1 :hp] 40))
          :dirty-arena test-arena}
         (#'move/apply-collision-damage
          (#'turns/get-decision-maker-data "1111-1111-1111-1111" test-arena)
          (get-item [6 4] test-arena)
          [6 4]
          10
          {:players test-players
           :dirty-arena test-arena}))
      "When a bot collides with another bot, damage is applied to both bots")
  ;; TODO This needs to be replaced since wombats can now be destroyed.
  #_(is (= {:players (-> test-players
                         (assoc-in [0 :hp] -20)
                         (assoc-in [1 :hp] 10))
            :dirty-arena test-arena}
           (#'move/apply-collision-damage
            (#'turns/get-decision-maker-data "1111-1111-1111-1111" test-arena)
            (get-item [6 4] test-arena)
            [6 4]
            40
            {:players test-players
             :dirty-arena test-arena}))
        "When a bot collides with another bot, and one or both bots go negitive with their score, neither bot will move, however, both will experience damage.")
  (is (= {:players (assoc-in test-players [0 :hp] -10)
          :dirty-arena (update-cell (update-cell test-arena [5 3] (assoc b1 :hp -10)) [6 3] o)}
         (#'move/apply-collision-damage
          (#'turns/get-decision-maker-data "1111-1111-1111-1111" test-arena)
          b
          [5 3]
          30
          {:players test-players
           :dirty-arena test-arena}))
      "When a bot collides with a wall, damage is applied to both the wall and bot that collided with it. If the wall has zero or less hp after the collision, the bot will take the space of the wall.")
  (is (= {:players (assoc-in test-players [0 :hp] (- (:hp bot1-private) 10))
          :dirty-arena (update-cell (update-cell test-arena
                                                 [6 3]
                                                 (assoc b1 :hp (- (:hp b1) 10)))
                                    [5 3]
                                    (assoc b :hp (- (:hp b) 10)))}
         (#'move/apply-collision-damage
          (#'turns/get-decision-maker-data "1111-1111-1111-1111" test-arena)
          b
          [5 3]
          10
          {:players test-players
           :dirty-arena test-arena}))))
