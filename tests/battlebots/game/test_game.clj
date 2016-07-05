(ns battlebots.game.test-game
  (:require [battlebots.constants.arena :refer [arena-key]]
            [battlebots.game.utils :as gu]))

(def o (:open arena-key))
(def b (:block arena-key))
(def f (:food arena-key))
(def p (:poison arena-key))
(def bot1-private {:_id "1"
                   :uuid "1111-1111-1111-1111"
                   :login "oconn"
                   :bot-repo "bot"
                   :energy 20
                   :bot "{:commands [{:cmd \"MOVE\"
                                      :metadata {:direction (rand-nth [0])}}
                                     {:cmd \"SET_STATE\"
                                      :metadata {:step-counter 0}}]}"
                   :saved-state {}})
(def bot2-private {:_id "2"
                   :uuid "2222-2222-2222-2222"
                   :login "Mr. Robot"
                   :bot-repo "bot"
                   :energy 50
                   :bot "{:commands [{:cmd \"MOVE\"
                                      :metadata {:direction (rand-nth [0])}}
                                     {:cmd \"SET_STATE\"
                                      :metadata {:step-counter 0}}]}"
                   :saved-state {}})
(def b1 (gu/sanitize-player bot1-private))
(def b2 (gu/sanitize-player bot2-private))
(def test-players [bot1-private bot2-private])

;; NOTE: do NOT modify this arena. Almost all of the following tests
;; rely on it and will most likey break all off them if it is modified.
(def test-arena [[o o b f p f f]
                 [b f f p o o o]
                 [b f o o p f p]
                 [b f f p o b b1]
                 [b p o o o p b2]
                 [p o p f f f f]
                 [o o o o f p f]])

(def test-game-state {:initial-arena test-arena
                      :clean-arena test-arena
                      :dirty-arena test-arena
                      :rounds []
                      :segment-count 0
                      :_id "1"
                      :players [bot1-private bot2-private]})
