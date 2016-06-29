(ns battlebots.arena.utils
  (:require [battlebots.constants.arena :refer [arena-key]]
            [clojure.string :as string]))

(defn get-item
  "gets an item based off of given coords"
  [[x y] arena]
  (get-in arena [y x]))

(defn get-arena-dimensions
  "returns the dimensions of a given arena (NOTE: Not 0 based)"
  [arena]
  (let [x (count arena)
        y ((comp count first) arena)]
    [x y]))

(defn update-cell
  "set the value of an arena cell to the value provided"
  [arena [x y] v]
  (let [[xdim ydim] (get-arena-dimensions arena)]
    (if (or (>= x xdim) (>= y ydim))
      arena
      (assoc-in arena [y x] v))))

(defn get-arena-row-cell-length
  "Similar to arena/get-arena-dimensions except that it is zero based"
  [arena]
  (map dec (get-arena-dimensions arena)))

(defn wrap-coords
  "wraps the coords around to the other side of the arena"
  [[c-x c-y] [d-x d-y]]
  (let [x (cond
            (< c-x 0) (if (> (Math/abs c-x) d-x)
                        (- d-x (mod (Math/abs c-x) d-x))
                        (+ d-x c-x))
            (>= c-x d-x) (mod c-x d-x)
           :else c-x)
        y (cond
            (< c-y 0) (if (> (Math/abs c-y) d-y)
                        (- d-y (mod (Math/abs c-y) d-y))
                        (+ d-y c-y))
            (>= c-y d-y) (mod c-y d-y)
            :else c-y)]
    [x y]))

(defn- incx [x] (fn [v] (+ x v)))

(defn adjust-coords
  "Returns a new set of coords based off of an applied direction.

  0 1 2
  7   3
  6 5 4

  "
  ([coords direction dimensions] (adjust-coords coords direction dimensions 1))
  ([coords direction dimensions steps]
   (let [
         updater (cond
                   (= direction 0) [(incx (- steps)) (incx (- steps))]
                   (= direction 1) [identity (incx (- steps))]
                   (= direction 2) [(incx steps) (incx (- steps))]
                   (= direction 3) [(incx steps) identity]
                   (= direction 4) [(incx steps) (incx steps)]
                   (= direction 5) [identity (incx steps)]
                   (= direction 6) [(incx (- steps)) (incx steps)]
                   (= direction 7) [(incx (- steps)) identity]
                   :else [identity identity])
         coords (map #(%1 %2) updater coords)]
     (wrap-coords coords dimensions))))

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

(defn get-arena-area
  [arena [posx posy] radius]
  (let [[xdim ydim] (get-arena-dimensions arena)
        [xmax ymax] (map dec [xdim ydim])
        maxradius (int (/ (dec xdim) 2))
        nradius (if (or (> (inc (* 2 radius)) xdim) (= radius 0))
                  maxradius
                  radius)
        [x1 y1] (adjust-coords [posx posy] 0 [xmax ymax] nradius)
        [x2 y2] (adjust-coords [posx posy] 4 [xmax ymax] nradius)
        ;; now take these coords and copy the ranges of the arena vectors
        ;; swapping as needed
        columns (if (> x2 x1)
                  (subvec arena x1 (inc x2))
                  (vec (concat (subvec arena (inc x1) xdim) (subvec arena 0 (inc x2)))))
        area (map (fn [col]
                    (if (> y2 y1)
                      (subvec col y1 (inc y2))
                      (vec (concat (subvec col (inc y1) ydim)
                                   (subvec col 0 (inc y2)))))) columns)]
    area))

(defn pprint-arena
  "Pretty Print for a given arena"
  [arena]
  (let [[x-len _] (get-arena-dimensions arena)
        x-indices (range x-len)]
    (println " " (string/join " " (vec x-indices)))
    (print
     (string/join "\n" (map-indexed (fn [idx row]
                                      (print
                                       idx
                                       (string/join " " (map :display row))
                                       "\n")) arena)))))

(comment
  (require [battlebots.arena.generation :refer [empty-arena]])
  (defn test-draw-line
    [x1 y1 x2 y2]
    (let [arena (empty-arena (+ 5 (max x1 x2)) (+ 5 (max y1 y2)))
          line (draw-line x1 y1 x2 y2)
          res-arena (reduce (fn [a p] (update-cell a p {:display "*"})) arena line)]
      res-arena)))
