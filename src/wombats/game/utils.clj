(ns wombats.game.utils
  (:require [wombats.arena.utils :as au]))

(defn- is-player?
  ""
  ([value]
   (= (get-in value [:contents :type]) :wombat))
  ([value player-eid]
   (= (get-in value [:contents :player-eid]) player-eid)))

(defn get-player-coords
  [arena player-eid]

  (first
   (for [[y row] (map-indexed vector arena)
         [x val] (map-indexed vector row)
         :when (is-player? val player-eid)]
     [x y])))

(defn- wrap-coords
  "Wraps out-of-bounds coordinates (zero-based) to opposite edge of m x n arena"
  [[x y] [m n]]
  {:pre [(integer? x) (integer? y) (pos? m) (pos? n)]
   :post [(let [[x y] %] (and (<= 0 x m) (<= 0 y n)))]}
  [(mod x m) (mod y n)])

(defn- incx
  [x]
  (fn [v] (+ x v)))

(defn directional-functions
  "Returns the update functions to apply to a set of coords

   0 1 2
   7   3
   6 5 4"
  ([direction] (directional-functions direction 1))
  ([direction dist]
   (condp = direction
     0 [(incx (- dist)) (incx (- dist))]
     1 [identity (incx (- dist))]
     2 [(incx dist) (incx (- dist))]
     3 [(incx dist) identity]
     4 [(incx dist) (incx dist)]
     5 [identity (incx dist)]
     6 [(incx (- dist)) (incx dist)]
     7 [(incx (- dist)) identity]
     [identity identity])))

(defn adjust-coords
  "Returns a new set of coords based off of an applied direction."
  ([coords direction dimensions]
   (adjust-coords coords direction dimensions 1))
  ([coords direction dimensions dist]
   (let [updater (directional-functions direction dist)
         coords (map #(%1 %2) updater coords)]
     (wrap-coords coords dimensions))))

(defn- normalize-slope
  [coords start]
  (if (= (vec (first coords)) start)
    (vec coords)
    (vec (reverse coords))))

(defn draw-line
  "Draw a line from x1,y1 to x2,y2 using Bresenham's line algorithm.

  Modified from http://rosettacode.org/wiki/Bitmap/Bresenham%27s_line_algorithm#Clojure"
  [from to]
  (let [[x1 y1] from
        [x2 y2] to
        dist-x (Math/abs (- x1 x2))
        dist-y (Math/abs (- y1 y2))
        steep (> dist-y dist-x)]
    (let [[x1 y1 x2 y2] (if steep
                          [y1 x1 y2 x2]
                          [x1 y1 x2 y2])]
      (let [[x1 y1 x2 y2] (if (> x1 x2)
                            [x2 y2 x1 y1]
                            [x1 y1 x2 y2])]
        (let  [delta-x (- x2 x1)
               delta-y (Math/abs (- y1 y2))
               y-step (if (< y1 y2) 1 -1)]
          (loop [x x1
                 y y1
                 error (Math/floor (/ delta-x 2))
                 plots [[x y]]]
            (if (< x x2)
              (let [[x y err] (if (< error delta-y)
                                [(inc x) (+ y y-step) (+ error (- delta-x delta-y))]
                                [(inc x) y (- error delta-y)])]
                (recur x y err (conj plots [x y])))
              (if steep
                (normalize-slope (map (fn [[y x]] [x y]) plots) from)
                (normalize-slope plots from)))))))))
