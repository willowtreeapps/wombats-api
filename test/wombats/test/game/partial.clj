(ns wombats.test.game.partial
  (:require [clojure.test :refer :all]
            [wombats.game.utils :as gu]
            [wombats.game.partial :as p]))

(defonce test-10x10-2-wombat-arena
  (load-file "resources/arena/10x10-2-wombat-arena.edn"))

(deftest get-partial-arena
  (testing "Returns a subset of an arena"
    (is (= [3 3]
           (gu/get-player-coords
            (p/get-partial-arena {:frame {:frame/arena (load-file "resources/arena/10x10-2-wombat-arena.edn")}
                                  :arena-config {:arena/width 10
                                                 :arena/height 10}}
                                 [3 8])
            17592186046374)))))
