(ns wombats.game.partial
  (:require [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]))

(defn calculate-box-coords
  "Get the box coordinates around a given coordinate"
  [[origin-x origin-y] [dim-x dim-y] radius]
  (let [[x1 y1] (gu/adjust-coords [origin-x origin-y] 0 [dim-x dim-y] radius)
        [x2 y2] (gu/adjust-coords [origin-x origin-y] 4 [dim-x dim-y] radius)]
    [x1 y1 x2 y2]))

(defn- calculate-safe-radius
  "Returns a radius that is safe to use in a given arena.
  A radius doubled that is larger than the shortest dimension
  is an invalid radius."
  [dimensions requested-radius]
  (let [dim (apply min dimensions)
        max-radius (dec (int (/ dim 2)))]
    (if (or (> (* 2 requested-radius) dim)
            (= requested-radius 0))
      max-radius
      requested-radius)))

(defn- slice-arena-vector
  [arena-vec stop-one stop-two]
  (if (< stop-one stop-two)
    (vec (subvec (vec arena-vec) stop-one (inc stop-two)))
    (let [calculated-stop (+ stop-two (count arena-vec))
          padded-vec (vec (concat arena-vec arena-vec))]
      (vec (subvec padded-vec stop-one (inc calculated-stop))))))

(defn get-partial-arena
  [game-state
   coords
   decision-maker-type]
  (let [arena (get-in game-state [:game/frame :frame/arena])
        {x :arena/width
         y :arena/height
         radius :arena/player-radius} (:game/arena game-state)
        ;; TODO move radius into config
        safe-radius (calculate-safe-radius [x y] 3)
        [x1 y1 x2 y2] (calculate-box-coords coords [x y] safe-radius)
        rows (slice-arena-vector arena y1 y2)
        area (vec (map #(slice-arena-vector % x1 x2) rows))]
    area))
