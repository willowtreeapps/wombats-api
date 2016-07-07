(ns battlebots.game.decisions.resolve-shot-spec
  (:require [battlebots.game.decisions.resolve-shot :refer :all :as resolve-shot]
            [battlebots.constants.arena :as ac])
  (:use clojure.test))

(deftest shot-should-progress-spec
  (is (= false (#'resolve-shot/shot-should-progress? false (:open ac/arena-key) 10))
      "Returns false when should-progress? prop is false")
  (is (= false (#'resolve-shot/shot-should-progress? true (:open ac/arena-key) 0))
      "Returns false when energy is 0")
  (is (= false (#'resolve-shot/shot-should-progress? true (:open ac/arena-key) -10))
      "Returns false when energy is less than 0")
  (is (= false (#'resolve-shot/shot-should-progress? true (:steel ac/arena-key) 10))
      "Returns false when encountering an cell it cannot pass through")
  (is (= true (#'resolve-shot/shot-should-progress? true (:open ac/arena-key) 10))
      "Returns true when all of the above test cases return true"))

(deftest add-shot-damage
  (is (= 10 (:energy ((#'resolve-shot/add-shot-damage 10) (:block ac/arena-key)))))
  (is (= 5 (:energy ((#'resolve-shot/add-shot-damage 15) (:block ac/arena-key))))))
