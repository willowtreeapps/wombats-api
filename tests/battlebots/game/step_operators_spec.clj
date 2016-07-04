(ns battlebots.game.step-operators-spec
  (:require [battlebots.game.utils :as gu]
            [battlebots.arena.utils :as au]
            [battlebots.game.step-operators :refer :all :as step-operators]
            [battlebots.game.test-game :refer [test-players
                                               test-game-state
                                               test-arena
                                               o b1]])
  (:use clojure.test))

(deftest randomize-players-spec
  (is (= (count test-players)
         (count (set (repeatedly 100 (partial #'step-operators/randomize-players test-players)))))
      "Players are randomized. NOTE: While not likely, this test could fail if one permutation
  is not calculated."))

(deftest process-command-spec
  (is (= 50 (:remaining-time ((#'step-operators/process-command "1" {:SHOOT {:tu 50}})
                              {:game-state test-game-state
                               :remaining-time 100} {:cmd "SHOOT"
                                                     :metadata {:energy 5
                                                                :direction 4}})))
      "When a player passes a command and has enough banked time to execute the command, remaining-time is decremented")
  (is (= 50 (:remaining-time ((#'step-operators/process-command "1" {:SHOOT {:tu 60}})
                              {:game-state test-game-state
                               :remaining-time 50} {:cmd "SHOOT"
                                                    :metadata {:energy 5
                                                               :direction 4}})))
      "When a player passas a command and does not have enough time to execute the command, remaining time does not change.")
  (is (= {:game-state test-game-state
          :remaining-time 20}
         ((#'step-operators/process-command "1" {:SHOOT {:tu 10}})
          {:game-state test-game-state
           :remaining-time 20} {:cmd "SOME_INVALID_COMMAND"
                                :metadata {}}))
      "When a player passes an invalid command, nothing is modified"))

(deftest apply-decisions-spec
  (is (= (gu/get-player-coords
          "1"
          (au/update-cell
           (au/update-cell
            (au/update-cell
             (au/update-cell
              test-arena
              [6 3] o)
             [6 2] o)
            [6 1] o)
           [6 0] b1))
         (gu/get-player-coords
          "1"
          (:dirty-arena ((#'step-operators/apply-decisions {:MOVE {:tu 33}}) test-game-state
                         {:decision {:commands [{:cmd "MOVE"
                                                 :metadata {:direction 1}}
                                                {:cmd "MOVE"
                                                 :metadata {:direction 1}}
                                                {:cmd "MOVE"
                                                 :metadata {:direction 1}}]}
                          :_id "1"}))))
      "Player 1 is moved 3 spaces up when passed 3 {:MOVE 1} commands, each costing 33 time units")
  (is (= (gu/get-player-coords
          "1"
          (au/update-cell
            (au/update-cell
             (au/update-cell
              test-arena
              [6 3] 0)
             [6 2] o)
            [6 1] b1))
         (gu/get-player-coords
          "1"
          (:dirty-arena ((#'step-operators/apply-decisions {:MOVE {:tu 50}}) test-game-state
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
