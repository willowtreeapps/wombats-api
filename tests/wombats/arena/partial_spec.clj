(ns wombats.arena.partial_spec
  (:require [wombats.constants.arena :refer [arena-key]]
            [wombats.arena.utils :as au]
            [wombats.arena.partial :refer :all :as partial])
  (:use clojure.test))

(def open-space (:open arena-key))
(def block-space (:block arena-key))
(def food-space (:food arena-key))
(def poison-space (:poison arena-key))

(def test-arena [[open-space block-space food-space food-space]
                 [open-space open-space block-space poison-space]
                 [block-space block-space food-space poison-space]
                 [open-space open-space food-space poison-space]])

(deftest get-arena-area-spec
  (is (= [[open-space block-space food-space]
          [open-space open-space block-space]
          [block-space block-space food-space]]
         (get-arena-area test-arena [1 1] 1)))
  (is (= [[block-space food-space food-space]
          [open-space block-space poison-space]
          [block-space food-space poison-space]]
         (get-arena-area test-arena [2 1] 1))))
