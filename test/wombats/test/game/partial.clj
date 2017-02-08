(ns wombats.test.game.partial
  (:require [clojure.test :refer :all]
            [wombats.game.utils :as gu]
            [wombats.game.partial :as p]))

(defonce test-10x10-2-wombat-arena
  (load-file "resources/arena/10x10-2-wombat-arena.edn"))

(prn test-10x10-2-wombat-arena)

(deftest get-partial-arena
  (testing "Returns a subset of an arena"
    (is (= [3 3]
           (gu/get-item-coords
            (p/get-partial-arena {:frame {:frame/arena (load-file "resources/arena/10x10-2-wombat-arena.edn")}
                                  :arena-config {:arena/width 10
                                                 :arena/height 10}}
                                 [3 8]
                                 :wombat)
            "10ca7ef0-67f1-4c2b-ac5b-7714203b55aa")))))
