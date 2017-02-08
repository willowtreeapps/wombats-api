(ns wombats.game.decisions.helpers)

(defn get-decision-maker-contents
  [decision-maker-state]
  (get-in decision-maker-state [:decision-maker :item :contents]))

(defn get-decision-maker-orientation
  [decision-maker-state]
  (:orientation (get-decision-maker-contents)))

(defn get-decision-maker-coords
  [decision-maker-state]
  (get-in decision-maker-state [:decision-maker :coords]))
