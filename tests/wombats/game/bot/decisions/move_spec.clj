(ns wombats.game.bot.decisions.move-spec
  (:require [wombats.game.bot.decisions.move :refer :all :as move]
            [wombats.arena.utils :refer [get-item update-cell]]
            [wombats.game.frame.turns :as turns]
            [wombats.constants.arena :as ac]
            [wombats.game.test-game :refer [o b
                                            bot1-private
                                            b1
                                            test-players
                                            test-arena]])
  (:use clojure.test))

;; TODO Test #move
