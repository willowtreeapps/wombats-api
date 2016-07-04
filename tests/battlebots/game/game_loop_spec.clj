(ns battlebots.game.game-loop-spec
  (:require [battlebots.game.game-loop :refer :all :as game-loop])
  (:use clojure.test))

(deftest total-rounds-spec
  (is (= 75 (#'game-loop/total-rounds 15 2)))
  (is (= 103 (#'game-loop/total-rounds 13 3))))
