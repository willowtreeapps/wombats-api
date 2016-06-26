(ns battlebots.arena-spec
  (:require [battlebots.utils.arena :refer :all]
            [battlebots.constants.arena :refer [arena-key]])
  (:use clojure.test))

(def open-space (:open arena-key))
(def block-space (:block arena-key))
(def food-space (:food arena-key))
(def poison-space (:poison arena-key))

(def test-arena [[open-space block-space food-space food-space]
                 [open-space open-space block-space poison-space]
                 [block-space block-space food-space poison-space]
                 [open-space open-space food-space poison-space]])

(deftest empty-arena-spec
  (is (= (empty-arena 2 2) [[open-space open-space] [open-space open-space]]))
  (is (= (empty-arena 1 2) [[open-space open-space]]))
  (is (= (empty-arena 2 1) [[open-space] [open-space]])))

(deftest get-item-spec
  (is (= (get-item [0 2] test-arena) food-space))
  (is (= (get-item [2 2] test-arena) food-space))
  (is (= (get-item [1 2] test-arena) block-space)))

(deftest get-arena-dimensions-spec
  (is (= (get-arena-dimensions [[0 0 0 0 0] [0 0 0 0 0] [0 0 0 0 0]]) [3 5]))
  (is (= (get-arena-dimensions [[0 0] [0 0]]) [2 2]))
  (is (= (get-arena-dimensions [[0]]) [1 1])))

(deftest get-arena-row-cell-length-spec
  (is (= (get-arena-row-cell-length test-arena) [3 3]))
  (is (= (get-arena-row-cell-length [[0 0 0 0 0 0]]) [0 5]))
  (is (= (get-arena-row-cell-length [[0 0 0 0 0 0] [0 0 0 0 0 0]]) [1 5])))

;; TODO Resolve wrap questions and pass commented tests
(deftest wrap-coords-spec
  (is (= (wrap-coords [0 0] [4 4]) [0 0])))

(deftest incx-spec
  (is (= ((incx 5) 1) 6))
  (is (= ((incx 2) 2) 4)))

;; Directions
;;
;; 0 1 2
;; 7   3
;; 6 5 4
;;
;; The following is a 4x4 arena showing the first two wrap around coords
;;
;; 22 32  02 12 22 32  02 12
;; 23 33  03 13 23 33  03 13
;;
;; 20 30  00 10 20 30  00 10
;; 21 31  01 11 21 31  01 11
;; 22 32  02 12 22 32  02 12
;; 23 33  03 13 23 33  03 13
;;
;; 20 30  00 10 20 30  00 10
;; 21 31  01 11 21 31  01 11
(deftest adjust-coords-spec
  (is (= (adjust-coords [2 2] 0 [4 4])   [1 1]) "move up and to the left one space")
  (is (= (adjust-coords [2 2] 0 [4 4] 2) [0 0]) "move up and to the left two spaces")
  (is (= (adjust-coords [2 2] 0 [4 4] 3) [3 3]) "move up and to the left three spaces")
  (is (= (adjust-coords [2 2] 0 [4 4] 4) [2 2]) "move up and to the left four spaces")
  (is (= (adjust-coords [2 2] 1 [4 4])   [2 1]) "move up one space")
  (is (= (adjust-coords [2 2] 1 [4 4] 4) [2 2]) "move up four spaces")
  (is (= (adjust-coords [2 2] 2 [4 4])   [3 1]) "move up and to the right one space")
  (is (= (adjust-coords [2 2] 2 [4 4] 3) [1 3]) "move up and to the right three spaces")
  (is (= (adjust-coords [2 2] 3 [4 4])   [3 2]) "move right one space")
  (is (= (adjust-coords [2 2] 3 [4 4] 2) [0 2]) "move right two spaces")
  (is (= (adjust-coords [2 2] 4 [4 4])   [3 3]) "move down and to the right one space")
  (is (= (adjust-coords [2 2] 4 [4 4] 2) [0 0]) "move down and to the right two spaces")
  (is (= (adjust-coords [2 2] 4 [4 4] 6) [0 0]) "move down and to the right 6 spaces")
  (is (= (adjust-coords [2 2] 5 [4 4])   [2 3]) "move down one space")
  (is (= (adjust-coords [2 2] 5 [4 4] 2) [2 0]) "move down two spaces")
  (is (= (adjust-coords [2 2] 5 [4 4] 3) [2 1]) "move down three spaces")
  (is (= (adjust-coords [2 2] 6 [4 4])   [1 3]) "move down and to the left one space")
  (is (= (adjust-coords [2 2] 6 [4 4] 9) [1 3]) "move down and to the left nine spaces")
  (is (= (adjust-coords [2 2] 7 [4 4])   [1 2]) "move left one space")
  (is (= (adjust-coords [2 2] 7 [4 4] 8) [2 2]) "move left eight spaces")
  (is (= (adjust-coords [2 2] 8 [4 4])   [2 2]) "passing an invalid direction will return the same coords")
  (is (= (adjust-coords [2 2] 0 [4 4] 0) [2 2]) "passing 0 for steps will return the same coords"))
