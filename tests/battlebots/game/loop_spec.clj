(ns battlebots.game.loop-spec
  (:require [battlebots.game.loop :refer :all :as game-loop])
  (:use clojure.test))

(deftest total-frames-spec
  (is (= 75 (#'game-loop/total-frames 15 2)))
  (is (= 103 (#'game-loop/total-frames 13 3))))
