(ns battlebots.game_spec
  (:require [battlebots.game :as game]
            [battlebots.arena.utils :refer [get-item update-cell]]
            [battlebots.constants.arena :refer [arena-key]])
  (:use clojure.test))

(def o (:open arena-key))
(def b (:block arena-key))
(def f (:food arena-key))
(def p (:poison arena-key))
(def bot1-private {:_id "1"
                    :login "oconn"
                    :bot-repo "bot"
                    :energy 20
                    :bot "{:commands [{:cmd \"MOVE\"
                                       :metadata {:direction (rand-nth [0])}}
                                      {:cmd \"SET_STATE\"
                                       :metadata {:step-counter 0}}]}"
                    :saved-state {}})
(def bot2-private {:_id "2"
                    :login "Mr. Robot"
                    :bot-repo "bot"
                    :energy 50
                    :bot "{:commands [{:cmd \"MOVE\"
                                       :metadata {:direction (rand-nth [0])}}
                                      {:cmd \"SET_STATE\"
                                       :metadata {:step-counter 0}}]}"
                   :saved-state {}})
(def b1 (#'game/sanitize-player bot1-private))
(def b2 (#'game/sanitize-player bot2-private))
(def test-players [bot1-private bot2-private])

;; NOTE: do NOT modify this arena. Almost all of the following tests
;; rely on it and will most likey break all off them if it is modified.
(def test-arena [[o o b f p f f]
                 [b f f p o o o]
                 [b f o o p f p]
                 [b f f p o b b1]
                 [b p o o o p b2]
                 [p o p f f f f]
                 [o o o o f p f]])

(def test-game-state {:initial-arena test-arena
                      :clean-arena test-arena
                      :dirty-arena test-arena
                      :rounds []
                      :segment-count 0
                      :_id "1"
                      :players [bot1-private bot2-private]})

(deftest position-spec
  (is (= 1 (#'game/position #(= % 4) [0 4 5 6])))
  (is (= 3 (#'game/position #(= % 6) [0 4 5 6])))
  (is (= nil (#'game/position #(= % 7) [0 4 5 6]))))

(deftest is-player-spec
  (is (= true (#'game/is-player? {:login "somelogin"})))
  (is (= false (#'game/is-player? {:no "login here"}))))

(deftest update-player-with-spec
  (is (= 1000 (:score (first (#'game/update-player-with "1"
                                                        test-players
                                                        {:score 1000}))))))

(deftest total-rounds-spec
  (is (= 75 (#'game/total-rounds 15 2)))
  (is (= 103 (#'game/total-rounds 13 3))))

(deftest get-player-spec
  (is (= (first test-players) (#'game/get-player "1" test-players))))

(deftest sanitize-player-spec
  (is (= {:_id "1"
          :login "oconn"
          :energy 100} (#'game/sanitize-player {:_id "1"
                                                :login "oconn"
                                                :energy 100
                                                :super-secret-value "shhhh...."}))))

(comment (deftest save-segment-spec))

(deftest randomize-players-spec
  (is (= (count test-players)
         (count (set (repeatedly 100 (partial #'game/randomize-players test-players))))))
  "Players are randomized. NOTE: While not likely, this test could fail if one permutation
  is not calculated.")

(deftest get-player-coords-spec
  (is (= [6 3] (#'game/get-player-coords "1" test-arena)))
  (is (= [6 4] (#'game/get-player-coords "2" test-arena))))

(deftest can-occupy-space-spec
  (is (= true (#'game/can-occupy-space? {:type "food"})) "Bots can occupy food spaces")
  (is (= true (#'game/can-occupy-space? {:type "poison"})) "Bots can occupy poison spaces")
  (is (= true (#'game/can-occupy-space? {:type "open"})) "Bots can occupy open spaces")
  (is (= false (#'game/can-occupy-space? {:type "block"}))) "Bots cannot occupy block spaces")

(comment (deftest determin-effects-spec
           (is (= {:energy #(+ % 10)} (#'game/determine-effects f)))))

(deftest apply-player-update-spec
  (is (= {:energy 5} (#'game/apply-player-update {:energy 10} {:energy #(- % 5)})))
  (is (= (assoc (first test-players) :energy 5)
         (#'game/apply-player-update (first test-players) {:energy #(- % 15)}))))

(deftest modify-player-state-spec
  (is (= (assoc-in test-players [0 :energy] 1)
         (#'game/modify-player-stats "1" {:energy #(- % 19)} test-players))))

(deftest apply-damage-spec
  (is (= o (#'game/apply-damage b 100))
      "Items are replaced with open spaces when they are destroyed.")
  (is (= (assoc b :energy (- (:energy b) 100)) (#'game/apply-damage b 100 false))
      "When false is passed as the last argument, the item is not replaced with an open space")
  (is (= (assoc b :energy (dec (:energy b))) (#'game/apply-damage b 1))
      "An item's energy is updated when damage is applied"))

;; TODO
(comment (deftest player-occupy-space-spec))

(deftest apply-collision-damage-spec
  (is (= {:players (assoc-in (assoc-in test-players [0 :energy] 10) [1 :energy] 40)
          :dirty-arena test-arena}
         ((#'game/apply-collision-damage "1" [6 3] (get-item [6 4] test-arena) [6 4] 10) {:players test-players
                                                                                          :dirty-arena test-arena}))
      "When a bot collides with another bot, damage is applied to both bots")
  (is (= {:players (assoc-in (assoc-in test-players [0 :energy] -20) [1 :energy] 10)
          :dirty-arena test-arena}
         ((#'game/apply-collision-damage "1" [6 3] (get-item [6 4] test-arena) [6 4] 40) {:players test-players
                                                                                          :dirty-arena test-arena}))
      "When a bot collides with another bot, and one or both bots go negitive with their score, neither bot will move, however, both will experience damage.")
  (is (= {:players (assoc-in test-players [0 :energy] -10)
          :dirty-arena (update-cell (update-cell test-arena [5 3] (assoc b1 :energy -10)) [6 3] o)}
         ((#'game/apply-collision-damage "1" [6 3] b [5 3] 30) {:players test-players
                                                                :dirty-arena test-arena}))
      "When a bot collides with a wall, damage is applied to both the wall and bot that collided with it. If the wall has zero or less energy after the collision, the bot will take the space of the wall.")
  (is (= {:players (assoc-in test-players [0 :energy] (- (:energy bot1-private) 10))
          :dirty-arena (update-cell (update-cell test-arena
                                                 [6 3]
                                                 (assoc b1 :energy (- (:energy b1) 10)))
                                    [5 3]
                                    (assoc b :energy (- (:energy b) 10)))}
         ((#'game/apply-collision-damage "1" [6 3] b [5 3] 10) {:players test-players
                                                                :dirty-arena test-arena}))))

(deftest process-command-spec
  (is (= 50 (:remaining-time ((#'game/process-command "1" {:SHOOT {:tu 50}})
                              {:game-state test-game-state
                               :remaining-time 100} {:cmd "SHOOT"
                                                     :metadata {:energy 5
                                                                :direction 4}})))
      "When a player passes a command and has enough banked time to execute the command, remaining-time is decremented")
  (is (= 50 (:remaining-time ((#'game/process-command "1" {:SHOOT {:tu 60}})
                              {:game-state test-game-state
                               :remaining-time 50} {:cmd "SHOOT"
                                                    :metadata {:energy 5
                                                               :direction 4}})))
      "When a player passas a command and does not have enough time to execute the command, remaining time does not change.")
  (is (= {:game-state test-game-state
          :remaining-time 20}
         ((#'game/process-command "1" {:SHOOT {:tu 10}})
          {:game-state test-game-state
           :remaining-time 20} {:cmd "SOME_INVALID_COMMAND"
                                :metadata {}}))
      "When a player passes an invalid command, nothing is modified"))

(deftest apply-decisions-spec
  (is (= (#'game/get-player-coords
          "1"
          (update-cell
           (update-cell
            (update-cell
             (update-cell
              test-arena
              [6 3] o)
             [6 2] o)
            [6 1] o)
           [6 0] b1))
         (#'game/get-player-coords
          "1"
          (:dirty-arena ((#'game/apply-decisions {:MOVE {:tu 33}}) test-game-state
                         {:decision {:commands [{:cmd "MOVE"
                                                 :metadata {:direction 1}}
                                                {:cmd "MOVE"
                                                 :metadata {:direction 1}}
                                                {:cmd "MOVE"
                                                 :metadata {:direction 1}}]}
                          :_id "1"}))))
      "Player 1 is moved 3 spaces up when passed 3 {:MOVE 1} commands, each costing 33 time units")
  (is (= (#'game/get-player-coords
          "1"
          (update-cell
            (update-cell
             (update-cell
              test-arena
              [6 3] 0)
             [6 2] o)
            [6 1] b1))
         (#'game/get-player-coords
          "1"
          (:dirty-arena ((#'game/apply-decisions {:MOVE {:tu 50}}) test-game-state
                         {:decision {:commands [{:cmd "MOVE"
                                                 :metadata {:direction 1}}
                                                {:cmd "MOVE"
                                                 :metadata {:direction 1}}
                                                {:cmd "MOVE"
                                                 :metadata {:direction 1}}
                                                {:cmd "MOVE"
                                                 :metadata {:direction 1}}
                                                {:cmd "MOVE"
                                                 :metadata {:direction 1}}]}
                          :_id "1"}))))
      "Player 1 is moved 2 spaces up when passed 5 {:MOVE 1} commands, each costing 50 time units"))
