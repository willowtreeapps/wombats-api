(ns wombats.game.frame.turns-spec
  (:require [wombats.game.frame.turns :as turns]
            [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]
            [wombats.game.test-game :refer [test-players
                                            test-game-state
                                            test-arena
                                            o f p b b1 b2]])
  (:use clojure.test))

(deftest process-command-spec
  (is (= 50 (:remaining-time ((#'turns/process-command "1111-1111-1111-1111" {:command-map {:SHOOT {:tu 50}}})
                              {:game-state test-game-state
                               :remaining-time 100} {:cmd "SHOOT"
                                                     :metadata {:hp 5
                                                                :direction 4}})))
      "When a player passes a command and has enough banked time to execute the command, remaining-time is decremented")
  (is (= 50 (:remaining-time ((#'turns/process-command "1111-1111-1111-1111" {:command-map {:SHOOT {:tu 60}}})
                              {:game-state test-game-state
                               :remaining-time 50} {:cmd "SHOOT"
                                                    :metadata {:hp 5
                                                               :direction 4}})))
      "When a player passas a command and does not have enough time to execute the command, remaining time does not change.")
  (is (= {:game-state test-game-state
          :remaining-time 20}
         ((#'turns/process-command "1111-1111-1111-1111" {:command-map {:SHOOT {:tu 10}}})
          {:game-state test-game-state
           :remaining-time 20} {:cmd "SOME_INVALID_COMMAND"
                                :metadata {}}))
      "When a player passes an invalid command, nothing is modified"))

(deftest apply-decisions-spec
  (testing "Player 1 is moved 3 spaces up when passed 3 {:MOVE 1} commands, each costing 33 time units"
    (is (= (gu/get-item-coords
            "1111-1111-1111-1111"
            (au/update-cell
             (au/update-cell
              (au/update-cell
               (au/update-cell
                test-arena
                [6 3] o)
               [6 2] o)
              [6 1] o)
             [6 0] b1))
           (gu/get-item-coords
            "1111-1111-1111-1111"
            (:dirty-arena ((#'turns/apply-decisions {:command-map {:MOVE {:tu 33}}
                                                      :initial-time-unit-count 100})
                           test-game-state
                           {:decision {:commands [{:cmd "MOVE"
                                                   :metadata {:direction 1}}
                                                  {:cmd "MOVE"
                                                   :metadata {:direction 1}}
                                                  {:cmd "MOVE"
                                                   :metadata {:direction 1}}]}
                            :uuid "1111-1111-1111-1111"}))))))
  (testing "Player 1 is moved 2 spaces up when passed 5 {:MOVE 1} commands, each costing 50 time units"
    (is (= (gu/get-item-coords
            "1111-1111-1111-1111"
            (au/update-cell
             (au/update-cell
              (au/update-cell
               test-arena
               [6 3] 0)
              [6 2] o)
             [6 1] b1))
           (gu/get-item-coords
            "1111-1111-1111-1111"
            (:dirty-arena ((#'turns/apply-decisions {:command-map {:MOVE {:tu 50}}
                                                      :initial-time-unit-count 100})
                           test-game-state
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
                            :uuid "1111-1111-1111-1111"})))))))
