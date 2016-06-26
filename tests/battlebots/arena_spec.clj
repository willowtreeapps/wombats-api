(ns battlebots.arena-spec
  (:require [battlebots.utils.arena :refer :all])
  (:use clojure.test))

(deftest get-arena-dimensions-spec
  (is (= [3 5] (get-arena-dimensions [[0 0 0 0 0] [0 0 0 0 0] [0 0 0 0 0]])))
  (is (= [2 2] (get-arena-dimensions [[0 0] [0 0]])))
  (is (= [1 1] (get-arena-dimensions [[0]]))))

(deftest get-arena-row-cell-length-spec
  (is (= [2 4] (get-arena-row-cell-length [[0 0 0 0 0] [0 0 0 0 0] [0 0 0 0 0]])))
  (is (= [1 1] (get-arena-row-cell-length [[0 0] [0 0]])))
  (is (= [0 0] (get-arena-row-cell-length [[0]]))))
