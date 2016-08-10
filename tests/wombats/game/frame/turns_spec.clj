(ns wombats.game.frame.turns-spec
  (:require [wombats.game.frame.turns :as turns]
            [wombats.config.game :as gc]
            [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]
            [wombats.game.test-game :refer [test-players
                                            test-game-state
                                            test-arena
                                            o f p b b1 b2]])
  (:use clojure.test))

;; TODO Test #resolve-turns
