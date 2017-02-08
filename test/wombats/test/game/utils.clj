(ns wombats.test.game.utils
  (:require [clojure.test :refer :all]
            [wombats.game.utils :as g-utils]
            [wombats.arena.utils :as au]))

(defonce test-10x10-2-wombat-arena
  (load-file "resources/arena/10x10-2-wombat-arena.edn"))

(deftest get-item-coords
  (testing "Returns a players coords when they exist in the arena"
    (is (= (g-utils/get-item-coords test-10x10-2-wombat-arena
                                    "10ca7ef0-67f1-4c2b-ac5b-7714203b55aa")
           [3 8]))))

(deftest adjust-coords
  (testing "Gets ajacent coords when no distance is passed"
    (is (= (g-utils/adjust-coords [5 5] 0 [10 10])
           [4 4]))
    (is (= (g-utils/adjust-coords [5 5] 1 [10 10])
           [5 4]))
    (is (= (g-utils/adjust-coords [5 5] 3 [10 10])
           [6 5])))
  (testing "Gets distant coords when a distance is passed"
    (is (= (g-utils/adjust-coords [5 5] 0 [10 10] 2)
           [3 3]))
    (is (= (g-utils/adjust-coords [5 5] 7 [10 10] 3)
           [2 5])))
  (testing "Wraps coords when reaching the end of a row / column"
    (is (= (g-utils/adjust-coords [0 0] 0 [10 10])
           [9 9]))
    (is (= (g-utils/adjust-coords [0 0] 0 [10 10] 2)
           [8 8]))
    (is (= (g-utils/adjust-coords [0 0] 1 [10 10] 10)
           [0 0]))
    (is (= (g-utils/adjust-coords [0 0] 2 [10 10] 11)
           [1 9]))))
