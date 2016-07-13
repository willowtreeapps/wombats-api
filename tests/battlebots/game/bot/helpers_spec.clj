(ns battlebots.game.bot.helpers-spec
  (:require [battlebots.game.bot.helpers :refer :all]
            [battlebots.game.test-game :refer [test-arena
                                               o f p b b1 b2 a]])
  (:use clojure.test))

(def ^:private ai (assoc a :uuid "1111-1111-1111-1111"))

(def ^:private sort-test-arena [[o  o  f  o  o  b  b]
                                [f  o  f  f  o  b  o]
                                [b1 o  o  f  o  f  f]
                                [o  o  f  f  f  b  o]
                                [o  o  f  f  ai o  f]
                                [o  o  b  f  b  o  f]
                                [o  o  b  f  b  o  f]])
(def ^:private sorted-test-arena (sort-arena sort-test-arena))

(deftest calculate-direction-from-origin-spec
  (is (= 0 (calculate-direction-from-origin [1 1] [0 0])))
  (is (= 7 (calculate-direction-from-origin [1 1] [0 1])))
  (is (= 4 (calculate-direction-from-origin [1 1] [2 2])))
  (is (= 5 (calculate-direction-from-origin [1 1] [1 2])))
  (is (= nil (calculate-direction-from-origin [1 1] [3 3])) "Returns nil if coords are not adjacent")
  (is (= nil (calculate-direction-from-origin [1 1] [0 3])) "Returns nil if coords are not adjacent"))

(deftest within-n-spaces-spec
  (is (= {:1 {:food   [{:match f :coords [3 3]}
                       {:match f :coords [4 3]}
                       {:match f :coords [3 4]}
                       {:match f :coords [3 5]}]
              :block  [{:match b :coords [5 3]}
                       {:match b :coords [4 5]}]
              :open   [{:match o :coords [5 4]}
                       {:match o :coords [5 5]}]}} (within-n-spaces sorted-test-arena [4 4] 1)))
  (is (= {:1 {:food   [{:match f :coords [0 1]}
                       {:match f :coords [2 1]}
                       {:match f :coords [2 3]}]
              :open   [{:match o :coords [1 1]}
                       {:match o :coords [2 2]}
                       {:match o :coords [0 3]}
                       {:match o :coords [1 3]}]
              :player [{:match b1 :coords [0 2]}]}} (within-n-spaces sorted-test-arena [1 2] 1))
      "Searching within a 1 space radius will return a sorted map of items within one space")
  (is (= {:1 {:open   [{:match o :coords [1 3]}
                       {:match o :coords [1 4]}
                       {:match o :coords [1 5]}]
              :food   [{:match f :coords [2 3]}
                       {:match f :coords [3 3]}
                       {:match f :coords [3 4]}
                       {:match f :coords [3 5]}]
              :block  [{:match b :coords [2 5]}]}
          :2 {:player [{:match b1 :coords [0 2]}]
              :open   [{:match o :coords [1 2]}
                       {:match o :coords [2 2]}
                       {:match o :coords [4 2]}
                       {:match o :coords [0 3]}
                       {:match o :coords [0 4]}
                       {:match o :coords [0 5]}
                       {:match o :coords [0 6]}
                       {:match o :coords [1 6]}]
              :food   [{:match f :coords [3 2]}
                       {:match f :coords [4 3]}
                       {:match f :coords [3 6]}]
              :ai     [{:match ai :coords [4 4]}]
              :block  [{:match b :coords [4 5]}
                       {:match b :coords [2 6]}
                       {:match b :coords [4 6]}]}} (within-n-spaces sorted-test-arena [2 4] 2))
      "When increasing the radius to two, the map includes items two spaces away"))

(deftest get-items-coords-spec
  (is (= [4 4] (get-items-coords ai sort-test-arena)))
  (is (= [1 2] (get-items-coords ai [[o  o  o]
                                     [o  o  o]
                                     [o  ai o]])))
  (is (= [0 2] (get-items-coords ai [[o  o  o]
                                     [o  o  o]
                                     [ai o  o]]))))

(deftest sort-arena-spec
  (is (= {:open   [{:match o :coords [0 0]}
                   {:match o :coords [2 0]}
                   {:match o :coords [2 2]}]
          :block  [{:match b :coords [1 0]}
                   {:match b :coords [0 1]}
                   {:match b :coords [1 1]}]
          :food   [{:match f :coords [2 1]}]
          :ai     [{:match ai :coords [0 2]}]
          :poison [{:match p :coords [1 2]}]} (sort-arena [[o  b  o]
                                                           [b  b  f]
                                                           [ai p  o]])))
  (is (= {:open [{:match o :coords [0 0]}]
          :food [{:match f :coords [1 0]}
                 {:match f :coords [1 1]}]
          :ai   [{:match ai :coords [0 1]}]} (sort-arena [[o  f]
                                                          [ai f]]))))

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

(deftest draw-line-spec
  (is (= [[0 0] [1 1] [2 2] [3 3]] (draw-line [0 0] [3 3])) "from (0 0) to (3 3)")
  #_(is (= [[2 2] [2 3] [2 4] [2 5]] (draw-line [2 2] [2 5])) "from (2 2) to (2 5)"))
