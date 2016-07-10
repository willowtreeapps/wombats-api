(ns battlebots.game.bot-helpers-spec
  (:require [battlebots.game.bot-helpers :refer :all]
            [battlebots.game.test-game :refer [test-arena
                                               o f p b b1 b2]])
  (:use clojure.test))

(deftest sort-arena-spec
  (is (= {:open   [{:match o :coords [0 0]}
                   {:match o :coords [0 1]}
                   {:match o :coords [1 2]}
                   {:match o :coords [2 2]}]
          :block  [{:match b :coords [1 0]}
                   {:match b :coords [2 0]}]
          :food   [{:match f :coords [1 1]}
                   {:match f :coords [2 1]}]
          :player [{:match b1 :coords [0 2]}]}
         (sort-arena [[o  b  b]
                      [o  f  f]
                      [b1 o  o]]))
      "Sorts a given arena"))

(deftest scan-for-spec
  (is (= [{:match f :coords [1 1]}
          {:match f :coords [2 1]}
          {:match f :coords [0 2]}] (scan-for
                                     #(= (:type f) (:type %))
                                     [[o  o  b]
                                      [b1 f  f]
                                      [f  o  b2]]))
      "Finds all food in the given arena")
  (is (= [{:match b :coords [2 0]}] (scan-for
                                     #(= (:type b) (:type %))
                                     [[o  o  b]
                                      [b1 f  f]
                                      [f  o  b2]]))
      "Finds all blocks in the given arena")
  (is (= [] (scan-for
             #(= (:type p) (:type %))
             [[o  o  b]
              [b1 f  f]
              [f  o  b2]]))
      "Finds no matches when nothing matches the predicate"))
