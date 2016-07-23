(ns wombats.constants.game)

;; Number of frames in a chunked segment
(def segment-length 30)
;; Number of frames in a game
(def game-length 120)
;; Ammount of damage that occurs when a collsion occurs
(def collision-damage-amount 10)
;; Command map
(def command-map {:MOVE {:tu 100}
                  :SHOOT {:tu 50}
                  :SET_STATE {:tu 0}})
;; Number of time units each player receives at the start of their turn
(def initial-time-unit-count 100)
;; Radius of the partial arena passed to each player
(def partial-arena-radius 10)
;; Radius of the partial arena passed to each AI bot
(def ai-partial-arena-radius 2)
