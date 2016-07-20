(ns battlebots.arena.partial
  (:require [battlebots.arena.utils :as au]))

(defn- caclulate-nradius
  [[x-dim y-dim] requested-radius]
  (let [max-radius (int (/ x-dim 2))]
    (if (or (> (* 2 requested-radius) x-dim)
            (= requested-radius 0))
      max-radius
      requested-radius)))

(defn- calculate-box-coords
  [origin-x origin-y dim-x dim-y radius]
  (let [[x1 y1] (au/adjust-coords [origin-x origin-y] 0 [dim-x dim-y] radius)
        [x2 y2] (au/adjust-coords [origin-x origin-y] 4 [dim-x dim-y] radius)]
    [x1 y1 x2 y2]))

(defn- slice-arena-vector
  [arena-vec stop-one stop-two]
  (if (< stop-one stop-two)
    (vec (subvec arena-vec stop-one (inc stop-two)))
    (let [calculated-stop (+ stop-two (count arena-vec))
          padded-vec (vec (concat arena-vec arena-vec))]
      (vec (subvec padded-vec stop-one (inc calculated-stop))))))

(defn get-arena-area
  "returns a partial arena centered on a given coordinate"
  [arena [pos-x pos-y] radius]
  (let [[x y] (au/get-arena-dimensions arena)
        nradius (caclulate-nradius [x y] radius)
        [x1 y1 x2 y2] (calculate-box-coords pos-x pos-y x y nradius)
        rows (slice-arena-vector arena y1 y2)
        area (vec (map #(slice-arena-vector % x1 x2) rows))]
    area))
