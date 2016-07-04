(ns battlebots.game.bot-decisions
  (:require [battlebots.game.decisions.move-player :refer [move-player]]
            [battlebots.game.decisions.save-state :refer [set-player-state]]))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; DECISION FUNCTIONS
;;
;; Each decision function takes the _id (of the user / bot
;; making the decision, the decision map, and the game-state.
;; Once the decision is applied, the decision function will
;; return a modifed game-state (:dirty-arena)
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def move move-player)
(def save-state set-player-state)
