(ns wombats.game.decisions.helpers)

(defn get-decision-maker-contents
  [decision-maker-state]
  (get-in decision-maker-state [:decision-maker :item :contents]))

(defn get-decision-maker-orientation
  [decision-maker-state]
  (:orientation (get-decision-maker-contents decision-maker-state)))

(defn get-decision-maker-coords
  [decision-maker-state]
  (get-in decision-maker-state [:decision-maker :coords]))

(defn get-arena-dimensions
  [game-state]
  (let [{width :arena/width
         height :arena/height} (:game/arena game-state)]
    [width height]))
