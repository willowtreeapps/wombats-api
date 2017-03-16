(ns wombats.game.decisions.turn
  (:require [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]
            [wombats.game.decisions.helpers :as dh]))

(defn turn
  [game-state metadata decision-maker-state]

  (let [contents (dh/get-decision-maker-contents decision-maker-state)
        orientation (:orientation contents)
        new-orientation (gu/modify-orientation orientation
                                               (keyword (:direction metadata)))
        coords (get-in decision-maker-state [:decision-maker
                                             :coords])
        updated-contents (assoc contents :orientation new-orientation)]

    (update-in game-state
               [:game/frame :frame/arena]
               (fn [arena]
                 (au/update-cell-contents arena
                                          coords
                                          updated-contents)))))
