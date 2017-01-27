(ns wombats.test.arena.utils
  (:require [clojure.test :refer :all]
            [wombats.arena.utils :as a-utils]))

(defonce test-4x4-empty-perimeter
  (load-file "resources/arena/4x4-empty-perimeter.edn"))

(defonce test-4x5-empty-perimeter
  (load-file "resources/arena/4x5-empty-perimeter.edn"))

(deftest get-arena-dimensions
  (testing "calculates a square arena"
    (is (= [4 4]
           (a-utils/get-arena-dimensions test-4x4-empty-perimeter))))

  (testing "calculates a non square  arena"
    (is (= [4 5]
           (a-utils/get-arena-dimensions test-4x5-empty-perimeter)))))

(deftest pos-open?
  (testing "returns true when it encounters an open cell"
    (is (= true
           (a-utils/pos-open?
            [1 1]
            test-4x4-empty-perimeter))))
  (testing "returns false when it encounters any other cell"
    (is (= false
           (a-utils/pos-open?
            [0 0]
            test-4x4-empty-perimeter)))))

(deftest coords-inbounds?
  (testing "returns true if coords are inbounds"
    (is (= (a-utils/coords-inbounds? [0 0] test-4x4-empty-arena)
           true))
    (is (= (a-utils/coords-inbounds? [0 3] test-4x4-empty-arena)
           true))
    (is (= (a-utils/coords-inbounds? [2 3] test-4x4-empty-arena)
           true)))
  (testing "returns false if coords are out of bounds"
    (is (= (a-utils/coords-inbounds? [4 4] test-4x4-empty-arena)
           false))))

(deftest update-cell
  (testing "returns the same arena if x is out of bounds"
    (is (= test-4x4-empty-arena
           (a-utils/update-cell test-4x4-empty-arena
                                [8 0]
                                {:update true}))))
  (testing "returns the same arena if y is out of bounds"
    (is (= test-4x4-empty-arena
           (a-utils/update-cell test-4x4-empty-arena
                                [0 8]
                                {:update true}))))
  (testing "returns a modified arena if x & y are inbounds"
    (is (not= test-4x4-empty-arena
           (a-utils/update-cell test-4x4-empty-arena
                                [1 1]
                                {:update true})))))
