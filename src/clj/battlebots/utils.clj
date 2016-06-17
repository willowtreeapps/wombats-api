(ns battlebots.utils
  (:require [battlebots.arena :refer :all]))

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

;; arena utils

(defn min-
  "subtract a number from another, returning min if the result is less than min"
  [num1 num2 min]
  (let [res (- num1 num2)]
    (if (<= res min) min res)))

(defn max+
  "add a number to another, returning max if the result is greater than max"
  [num1 num2 max]
  (let [res (+ num1 num2)]
    (if (>= res max) max res)))

(defn get-area-in-range
  "given an arena, a position, and fov radius return an arena subset within range"
  [arena [posx posy] radius]
  (let [dims (get-arena-dimensions arena)
        x-bound (- (get dims 0) 1)
        y-bound (- (get dims 1) 1)
        x1 (min- posx radius 0)
        x2 (max+ posx radius x-bound)
        y1 (min- posy radius 0)
        y2 (max+ posy radius y-bound)]
    (map #(subvec % y1 y2) (subvec arena x1 x2))))

(defn get-wrapped-area-in-range
  "given an arena, a position, and fov radius return an arena subset within range"
  [arena [posx posy] radius]
  (let [dims (get-arena-dimensions arena)
        x-bound (- (get dims 0) 1)
        y-bound (- (get dims 1) 1)
        x1 (if (> radius posx)
             (- posx (mod radius posx))
             (- posx radius))
        x2 (if (> (+ posx radius) x-bound)
             (+ posx (mod (+ posx radius) x-bound))
             (+ posx radius))
        y1 (if (> radius posy)
             (- posy (mod radius posy))
             (- posy radius))
        y2 (if (> (+ posy radius) y-bound)
             (+ posy (mod (+ posy radius)) y-bound)
             (+ posy radius))]
    (map #(subvec % y1 y2) (subvec arena x1 x2))))

(defn draw-line
  "Draw a line from x1,y1 to x2,y2 using Bresenham's Algorithm"
  [x1 y1 x2 y2]
  (let [dist-x (Math/abs (- x1 x2))
        dist-y (Math/abs (- y1 y2))
        steep (> dist-y dist-x)]
    (let [[x1 y1 x2 y2] (if steep [y1 x1 y2 x2] [x1 y1 x2 y2])]
      (let [[x1 y1 x2 y2] (if (> x1 x2) [x2 y2 x1 y1] [x1 y1 x2 y2])]
        (let  [delta-x (- x2 x1)
               delta-y (Math/abs (- y1 y2))
               y-step (if (< y1 y2) 1 -1)]
          (loop [x x1
                 y y1
                 error (Math/floor (/ delta-x 2))
                 res []]
            (let [pt (if steep
                      [(int x) (int y)]
                      [(int y) (int x)])]
              (if (>= x x2)
                (conj res [x2 y2])
                ; Rather then rebind error, test that it is less than delta-y not 0
                (if (< error delta-y)
                  (recur (inc x) (+ y y-step) (+ error (- delta-x delta-y)) (conj res pt))
                  (recur (inc x) y            (- error delta-y) (conj res pt)))))))))))

(defn test-draw-line
  [x1 y1 x2 y2]
  (let [arena (empty-arena (+ 5 (max x1 x2)) (+ 5 (max y1 y2)))
        line (draw-line x1 y1 x2 y2)
        res-arena (reduce (fn [a p] (update-cell a p {:display "*"})) arena line)]
    res-arena))

(defn pretty-print-arena
  [arena]
  (doseq [a (apply map vector arena)] (println (map :display a))))
