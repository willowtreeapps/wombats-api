(ns battlebots.game.bot.decisions.shoot-spec
  (:require [battlebots.game.bot.decisions.shoot :refer :all :as shoot]
            [battlebots.constants.arena :as ac]
            [battlebots.arena.utils :as au]
            [battlebots.game.test-game :refer [o b
                                               bot1-private
                                               bot2-private
                                               b1 b2
                                               test-players
                                               test-arena]])
  (:use clojure.test))

(deftest add-shot-metadata-spec
  (is (= {:energy 10
          :md {:1234 {:type :shot
                      :decay 1}}}
         ((#'shoot/add-shot-metadata "1234") {:energy 10}))
      "Adds shot metadata to a cell")
  (is (= {:energy 12
          :md {:1234 {:type :shot
                      :decay 1}
               :5678 {:type :explosion
                      :decay 5}}}
         ((#'shoot/add-shot-metadata "1234") {:energy 12
                                              :md {:5678 {:type :explosion
                                                          :decay 5}}}))
      "Adds shot metadata to a cell already containing metadata"))

(deftest add-shot-damage-spec
  (is (= {:energy 190} ((#'shoot/add-shot-damage 10) {:energy 200})))
  (is (= {:energy 12} ((#'shoot/add-shot-damage 15) {:energy 27}))))

(deftest replace-destroyed-cell-spec
  (is (= (assoc o :md {:1234 {:type :destroyed
                              :decay 1}})
         ((#'shoot/replace-destroyed-cell "1234") (assoc b :energy 0)))
      "A cell is replaced if it is destroyed")
  (is (= b
         ((#'shoot/replace-destroyed-cell "1234") b))
      "A cell is not modified if it is not destroyed"))

(deftest resolve-shot-cell-spec
  (is (= {:energy 5
          :md {:1234 {:type :shot
                      :decay 1}}}
         (#'shoot/resolve-shot-cell {:energy 20} 15 "1234")))
  (is (= {:energy -10
          :md {:1234 {:type :shot
                      :decay 1}}}
         (#'shoot/resolve-shot-cell {:energy 20} 30 "1234"))))

(deftest shot-should-progress-spec
  (is (= false (#'shoot/shot-should-progress? false (:open ac/arena-key) 10))
      "Returns false when should-progress? prop is false")
  (is (= false (#'shoot/shot-should-progress? true (:open ac/arena-key) 0))
      "Returns false when energy is 0")
  (is (= false (#'shoot/shot-should-progress? true (:open ac/arena-key) -10))
      "Returns false when energy is less than 0")
  (is (= false (#'shoot/shot-should-progress? true (:steel ac/arena-key) 10))
      "Returns false when encountering an cell it cannot pass through")
  (is (= true (#'shoot/shot-should-progress? true (:open ac/arena-key) 10))
      "Returns true when all of the above test cases return true"))

(deftest update-victim-energy-spec
  (is (= {:players test-players
          :shooter-id "1"
          :cell b
          :damage 20}
         (#'shoot/update-victim-energy {:players test-players
                                        :shooter-id "1"
                                        :cell b
                                        :damage 20}))
      "No damage is applied if the cell is not a player")
  (is (= {:players [bot1-private
                    (assoc bot2-private :energy 30)]
          :shooter-id "1"
          :cell b2
          :damage 20}
         (#'shoot/update-victim-energy {:players test-players
                                        :shooter-id "1"
                                        :cell b2
                                        :damage 20}))
      "Damage is applied to the victim if the cell is a player"))

(deftest reward-shooter-spec
  (is (= {:players [(assoc bot1-private :energy 120)
                    bot2-private]
          :shooter-id "1"
          :cell b2
          :damage 50}
         (#'shoot/reward-shooter {:players test-players
                                  :shooter-id "1"
                                  :cell b2
                                  :damage 50}))
      "When a player strikes another player, they will recieve energy in the amount of 2x the damage applied to the victim.")
  (is (= {:players [(assoc bot1-private :energy 70)
                    bot2-private]
          :shooter-id "1"
          :cell b
          :damage 50}
         (#'shoot/reward-shooter {:players test-players
                                  :shooter-id "1"
                                  :cell b
                                  :damage 50}))
      "When a player strikes a wall, they will recieve energy in the amount of the damage applied to the wall.")
  (is (= {:players test-players
          :shooter-id "1"
          :cell o
          :damage 50}
         (#'shoot/reward-shooter {:players test-players
                                  :shooter-id "1"
                                  :cell o
                                  :damage 50}))
      "When a player strikes an open space, they will recieve no additional energy"))

(deftest process-shot-spec
  (is (= {:game-state {:dirty-arena (au/update-cell
                                     test-arena
                                     [0 1]
                                     (merge b {:energy 10
                                               :md {:1234 {:type :shot
                                                           :decay 1}}}))
                       :players test-players}
          :energy 0
          :should-progress? true
          :shot-uuid "1234"
          :shooter-id "99999"}
         (#'shoot/process-shot
          {:game-state {:dirty-arena test-arena
                        :players test-players}
           :energy 10
           :should-progress? true
           :shot-uuid "1234"
           :shooter-id "99999"}
          [0 1]))
      "If a shot passes through a cell what container more energy than is left in the shot. There should be no energy left over.")
  (is (= {:game-state {:dirty-arena (au/update-cell
                                     test-arena
                                     [0 1]
                                     (merge o {:md {:1234 {:type :destroyed
                                                           :decay 1}}}))
                       :players test-players}
          :energy 12
          :should-progress? true
          :shot-uuid "1234"
          :shooter-id "99999"}
         (#'shoot/process-shot
          {:game-state {:dirty-arena test-arena
                        :players test-players}
           :energy 32
           :should-progress? true
           :shot-uuid "1234"
           :shooter-id "99999"}
          [0 1]))
      "If a shot contains more energy than a cell has, the delta energy should be returned in the shot state.")
  (is (= {:game-state {:dirty-arena test-arena
                       :players test-players}
          :energy 32
          :should-progress? false
          :shot-uuid "1234"
          :shooter-id "99999"}
         (#'shoot/process-shot
          {:game-state {:dirty-arena test-arena
                        :players test-players}
           :energy 32
           :should-progress? false
           :shot-uuid "1234"
           :shooter-id "99999"}
          [0 1]))
      "If a shot should not progress, shot state is not updated"))
