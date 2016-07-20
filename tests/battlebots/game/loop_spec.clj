(ns battlebots.game.loop-spec
  (:require [battlebots.game.loop :refer :all :as game-loop])
  (:use clojure.test))

(deftest total-frames-spec
  (is (= 75 (#'game-loop/total-frames 15 2)))
  (is (= 103 (#'game-loop/total-frames 13 3))))

(deftest game-over-spec
  (is (= true (#'game-loop/game-over? {:segment-count 4
                                       :frames []})))
  (is (= true (#'game-loop/game-over? {:segment-count 3
                                       :frames (vec (range 30))})))
  (is (= false (#'game-loop/game-over? {:segment-count 3
                                        :frames []}))))

(deftest round-over-spec
  (is (= false (#'game-loop/round-over? {:frames (vec (range 29))})))
  (is (= false (#'game-loop/round-over? {:frames [0]}))
      "Returns false when the total frame count is less than the round length")
  (is (= true (#'game-loop/round-over? {:frames (vec (range 30))}))
      "Returns true when the frame length equals the round length"))
