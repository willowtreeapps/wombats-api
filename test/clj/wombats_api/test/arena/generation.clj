(ns wombats-api.test.arena.generation
  (:require [wombats-api.arena.generation :refer :all]
            [wombats-api.constants.arena :refer [arena-key]])
  (:use clojure.test))

(def open-space (:open arena-key))
(def block-space (:block arena-key))
(def food-space (:food arena-key))
(def poison-space (:poison arena-key))

(deftest empty-arena-spec
  (is (= [[open-space open-space] [open-space open-space]] (empty-arena 2 2)))
  (is (= [[open-space open-space]] (empty-arena 2 1)))
  (is (= [[open-space] [open-space]] (empty-arena 1 2))))
