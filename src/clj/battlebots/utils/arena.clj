(ns battlebots.utils.arena)

(defn get-item
  "gets an item based off of given coords"
  [coords arena]
  (get-in arena coords))

(defn get-arena-row-cell-length
  "Similar to arena/get-arena-dimensions except that it is zero based"
  [arena]
  (let [x (count arena)
        y ((comp count first) arena)]
    [(dec x) (dec y)]))

(defn wrap-coords
  "wraps the coords around to the other side of the arena"
  [[c-x c-y] [d-x d-y]]
  (let [x (cond
            (< c-x 0) (if (> (Math/abs c-x) d-x)
                        (- d-x (mod (Math/abs c-x) d-x))
                        (+ d-x c-x))
            (> c-x d-x) (mod c-x d-x)
           :else c-x)
        y (cond
            (< c-y 0) (if (> (Math/abs c-y) d-y)
                        (- d-y (mod (Math/abs c-y) d-y))
                        (+ d-y c-y))
            (> c-y d-y) (mod c-y d-y)
            :else c-y)]
    [x y]))

(defn incx
  [x]
  (fn [v] (+ x v)))

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
                   (= direction 1) [(incx (- steps)) identity]
                   (= direction 2) [(incx (- steps)) (incx steps)]
                   (= direction 3) [identity (incx steps)]
                   (= direction 4) [(incx steps) (incx steps)]
                   (= direction 5) [(incx steps) identity]
                   (= direction 6) [(incx steps) (incx (- steps))]
                   (= direction 7) [identity (incx (- steps))]
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

(defn test-draw-line
  [x1 y1 x2 y2]
  (let [arena (empty-arena (+ 5 (max x1 x2)) (+ 5 (max y1 y2)))
        line (draw-line x1 y1 x2 y2)
        res-arena (reduce (fn [a p] (update-cell a p {:display "*"})) arena line)]
    res-arena))

(defn pretty-print-arena
  [arena]
  (doseq [a (apply map vector arena)] (println (map :display a))))

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
