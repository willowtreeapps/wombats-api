(ns battlebots.arena.utils_spec
  (:require [battlebots.arena.utils :refer :all :as au]
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

(deftest get-item-spec
  (is (= block-space (get-item [0 2] test-arena)))
  (is (= food-space (get-item [2 2] test-arena)))
  (is (= poison-space (get-item [3 1] test-arena)))
  (is (= food-space (get-item [2 3] test-arena))))

(deftest get-arena-dimensions-spec
  (is (= [3 5] (get-arena-dimensions [[0 0 0 0 0] [0 0 0 0 0] [0 0 0 0 0]])))
  (is (= [2 2] (get-arena-dimensions [[0 0] [0 0]])))
  (is (= [1 1] (get-arena-dimensions [[0]]))))

(deftest update-cell-spec
  (is (= food-space (get-item [1 1] (update-cell test-arena [1 1] food-space))))
  (is (= block-space (get-item [1 1] (update-cell test-arena [1 1] block-space))))
  (is (= food-space (get-item [0 1] (update-cell test-arena [0 1] food-space)))))

(deftest get-arena-row-cell-length-spec
  (is (= [3 3] (get-arena-row-cell-length test-arena)))
  (is (= [0 5] (get-arena-row-cell-length [[0 0 0 0 0 0]])))
  (is (= [1 5] (get-arena-row-cell-length [[0 0 0 0 0 0] [0 0 0 0 0 0]]))))

(deftest wrap-coords-spec
  (is (= [0 0] (wrap-coords [0 0] [4 4])))
  (is (= [0 1] (wrap-coords [0 1] [4 4])))
  (is (= [0 0] (wrap-coords [4 4] [4 4])))
  (is (= [0 0] (wrap-coords [8 8] [4 4])))
  (is (= [3 1] (wrap-coords [3 9] [4 4])))
  (is (= [1 1] (wrap-coords [5 5] [4 4]))))

(deftest incx-spec
  (is (= 6 ((#'au/incx 5) 1)))
  (is (= 4 ((#'au/incx 2) 2))))

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
  (is (= [1 1] (adjust-coords [2 2] 0 [4 4]))   "move up and to the left one space")
  (is (= [0 0] (adjust-coords [2 2] 0 [4 4] 2)) "move up and to the left two spaces")
  (is (= [3 3] (adjust-coords [2 2] 0 [4 4] 3)) "move up and to the left three spaces")
  (is (= [2 2] (adjust-coords [2 2] 0 [4 4] 4)) "move up and to the left four spaces")
  (is (= [2 1] (adjust-coords [2 2] 1 [4 4]))   "move up one space")
  (is (= [2 2] (adjust-coords [2 2] 1 [4 4] 4)) "move up four spaces")
  (is (= [3 1] (adjust-coords [2 2] 2 [4 4]))   "move up and to the right one space")
  (is (= [1 3] (adjust-coords [2 2] 2 [4 4] 3)) "move up and to the right three spaces")
  (is (= [3 2] (adjust-coords [2 2] 3 [4 4]))   "move right one space")
  (is (= [0 2] (adjust-coords [2 2] 3 [4 4] 2)) "move right two spaces")
  (is (= [3 3] (adjust-coords [2 2] 4 [4 4]))   "move down and to the right one space")
  (is (= [0 0] (adjust-coords [2 2] 4 [4 4] 2)) "move down and to the right two spaces")
  (is (= [0 0] (adjust-coords [2 2] 4 [4 4] 6)) "move down and to the right 6 spaces")
  (is (= [2 3] (adjust-coords [2 2] 5 [4 4]))   "move down one space")
  (is (= [2 0] (adjust-coords [2 2] 5 [4 4] 2)) "move down two spaces")
  (is (= [2 1] (adjust-coords [2 2] 5 [4 4] 3)) "move down three spaces")
  (is (= [1 3] (adjust-coords [2 2] 6 [4 4]))   "move down and to the left one space")
  (is (= [1 3] (adjust-coords [2 2] 6 [4 4] 9)) "move down and to the left nine spaces")
  (is (= [1 2] (adjust-coords [2 2] 7 [4 4]))   "move left one space")
  (is (= [2 2] (adjust-coords [2 2] 7 [4 4] 8)) "move left eight spaces")
  (is (= [2 2] (adjust-coords [2 2] 8 [4 4]))   "passing an invalid direction will return the same coords")
  (is (= [2 2] (adjust-coords [2 2] 0 [4 4] 0)) "passing 0 for steps will return the same coords"))

(deftest draw-line-spec
  (is (= [[0 0] [1 1] [2 2] [3 3]] (draw-line 0 0 3 3)) "from (0 0) to (3 3)")
  #_(is (= [[2 2] [2 3] [2 4] [2 5]] (draw-line 2 2 2 5)) "from (2 2) to (2 5)"))
