(ns wombats.game.loop-spec
  (:require [wombats.game.loop :refer :all :as game-loop]
            [wombats.config.game :as game])
  (:use clojure.test))

(deftest game-over-spec
  (is (= true (#'game-loop/game-over? {:round-count 5
                                       :frames []}
                                      {:rounds-per-game 5})))
  (is (= false (#'game-loop/game-over? {:round-count 3
                                        :frames []}
                                       {:rounds-per-game 5}))))

(deftest round-over-spec
  (is (= false (#'game-loop/round-over? {:frames (vec (range 29))} {:frames-per-round 30})))
  (is (= false (#'game-loop/round-over? {:frames [0]} {:frames-per-round 30}))
      "Returns false when the total frame count is less than the round length")
  (is (= true (#'game-loop/round-over? {:frames (vec (range 30))} {:frames-per-round 30}))
      "Returns true when the frame length equals the round length"))
