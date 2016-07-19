(ns battlebots.game.test-game
  (:require [battlebots.game.utils :as gu]))

(def o {:type "open"
        :display " "
        :transparent true})
(def b {:type "block"
        :display "X"
        :transparent false
        :energy 20})
(def f {:type "food"
        :display "+"
        :transparent true})
(def p {:type "poison"
        :display "-"
        :transparent true})
(def a {:type "ai"
        :display "@"
        :transparent true
        :energy 20})

(def bot1-private {:_id "1"
                   :type "player"
                   :uuid "1111-1111-1111-1111"
                   :login "oconn"
                   :bot-repo "bot"
                   :energy 20
                   :messages []
                   :bot "{:commands [{:cmd \"MOVE\"
                                      :metadata {:direction (rand-nth [0])}}
                                     {:cmd \"SET_STATE\"
                                      :metadata {:step-counter 0}}]}"
                   :saved-state {}})
(def bot2-private {:_id "2"
                   :type "player"
                   :uuid "2222-2222-2222-2222"
                   :login "Mr. Robot"
                   :bot-repo "bot"
                   :energy 50
                   :messages []
                   :bot "{:commands [{:cmd \"MOVE\"
                                      :metadata {:direction (rand-nth [0])}}
                                     {:cmd \"SET_STATE\"
                                      :metadata {:step-counter 0}}]}"
                   :saved-state {}})
(def b1 (gu/sanitize-player bot1-private))
(def b2 (gu/sanitize-player bot2-private))
(def test-players [bot1-private bot2-private])

;; NOTE: do NOT modify this arena. Almost all of game tests
;; rely on it and will most likey break all off them if it is modified.
;; If modification is required, re-run tests and make necessary changes.
(def test-arena [[o o b f p f f]
                 [b f f p o o o]
                 [b f o o p f p]
                 [b f f a o b b1]
                 [b p o o o p b2]
                 [p o p f f f f]
                 [o o o o f p f]])

(def test-game-state {:initial-arena test-arena
                      :clean-arena test-arena
                      :dirty-arena test-arena
                      :frames []
                      :segment-count 0
                      :_id "1"
                      :messages []
                      :players [bot1-private bot2-private]})
