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
