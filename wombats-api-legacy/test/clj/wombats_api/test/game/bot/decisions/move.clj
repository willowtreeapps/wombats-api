(ns wombats-api.test.game.bot.decisions.move
  (:require [wombats-api.game.bot.decisions.move :refer :all :as move]
            [wombats-api.arena.utils :refer [get-item update-cell]]
            [wombats-api.game.frame.turns :as turns]
            [wombats-api.constants.arena :as ac]
            [wombats-api.test.game.test-game :refer [o b
                                                     bot1-private
                                                     b1
                                                     test-players
                                                     test-arena]])
  (:use clojure.test))

;; TODO Test #move
