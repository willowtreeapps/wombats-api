(ns wombats-api.test.game.frame.initiative
  (:require [wombats-api.game.frame.initiative :as initiative]
            [wombats-api.test.game.test-game :refer [test-players
                                                     test-game-state
                                                     test-arena
                                                     o f p b b1 b2]])
  (:use clojure.test))

(deftest update-initiative-order
  (is (= {:initiative-order '("1111-1111-1111-1111"
                              "2222-2222-2222-2222")
          :clean-arena test-arena}
         (initiative/update-initiative-order {:initiative-order '("2222-2222-2222-2222"
                                                                  "1111-1111-1111-1111")
                                              :clean-arena test-arena}))
      "Initiative Order is rotated"))
