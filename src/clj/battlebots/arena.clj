(ns battlebots.arena
  (:require [battlebots.constants.arena :refer [arena-key]]))

(defn arena-icon
  [key]
  (:display (key arena-key)))

;; ----------------------------------
;; MAP GENERATION HELPERS
;; ----------------------------------

(defn get-arena-dimensions
  "returns the dimensions of a given arena (NOTE: Not 0 based)"
  [arena]
  (let [x (count arena)
        y ((comp count first) arena)]
    [x y]))

(defn get-number-of-items
  "reutrns the number of items based off of a given frequency and the total number
  of cells in a given arena"
  [frequency arena]
  (* (/ frequency 100) (apply * (get-arena-dimensions arena))))

(defn pos-open
  "returns true of false depending if a given coodinate in a given arena is open"
  [[x y] arena]
  (= (:open arena-key) (get-in arena [x y])))

(defn generate-random-coords
  "generates random coordinates from a given dimension set"
  [[x y]]
  [(rand-int x) (rand-int y)])

(defn find-random-open-space
  "returns the coordinates for a random open space in a given arena"
  [arena]
  (let [arena-dimensions (get-arena-dimensions arena)]
    (loop [dimensions nil]
      (if (and dimensions (pos-open dimensions arena))
        dimensions
        (recur (generate-random-coords arena-dimensions))))))

(defn update-cell
  "set the value of an arena cell to the value provided"
  [arena [x y] v]
  (let [[xdim ydim] (get-arena-dimensions arena)]
    (if (or (>= x xdim) (>= y ydim))
      arena
      (assoc-in arena [x y] v))))

(defn replacer
  "replaces an empty cell with a value in a given arena"
  [arena item]
  (update-cell arena (find-random-open-space arena) item))

(defn sprinkle
  "sprinkles given items into an arena"
  [amount item arena]
  (reduce replacer arena (repeat amount item)))

(defn place-walls
  "places walls in an organized structures around the arena"
  [amount arena config]
  (let [wall-symbol (:block arena-key)]
    ;; TODO Update wall generation logic Github issue #10
    arena))
;; ----------------------------------
;; END MAP GENERATION HELPERS
;; ----------------------------------

;; ----------------------------------
;; FIELD OF VISION HELPERS
;; ----------------------------------
(defn get-area-in-range
  "given an arena, a position, and fov radius return an arena subset within range"
  [arena [posx posy] radius]
  (let [dims (get-arena-dimensions arena)
        x-bound (- (get dims 0) 1)
        y-bound (- (get dims 1) 1)
        x1 (min- posx radius 0)
        x2 (max+ posx radius x-bound)
        y1 (min- posy radius 0)
        y2 (min- posy radius y-bound)]
    (map #(subvec % y1 y2) (subvec arena x1 x2))))

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
                res
                ; Rather then rebind error, test that it is less than delta-y not 0
                (if (< error delta-y)
                  (recur (inc x) (+ y y-step) (+ error (- delta-x delta-y)) (conj res pt))
                  (recur (inc x) y            (- error delta-y) (conj res pt)))))))))))

(defn test-draw-line
  [x1 y1 x2 y2]
  (let [arena (empty-arena (+ 5 (max x1 x2)) (+ 5 (max y1 y2)))
        line (draw-line x1 y1 x2 y2)
        new-arena (reduce (fn [a p] (update-cell a p "*")) arena line)]))




;; ----------------------------------
;; END FIELD OF VISION HELPERS
;; ----------------------------------

;; ----------------------------------
;; ITEM DROP
;; ----------------------------------

;; TODO Update logic for each drop method. Right now we drop everything
;; randomly, this may not be desirable for all items but works for now.

(defn blocks
  "sprinkle blocks around the arena and return a new arena
  make sure there are no inaccessible areas"
  [frequency config arena]
  (let [amount (get-number-of-items frequency arena)]
    (place-walls amount arena config)))

(defn food
  "sprinkle food around the arena and return a new arena"
  [frequency config arena]
  (let [amount (get-number-of-items frequency arena)]
    (sprinkle amount (:food arena-key) arena)))

(defn poison
  "sprinkle poison around the arena and return a new arena"
  [frequency config arena]
  (let [amount (get-number-of-items frequency arena)]
    (sprinkle amount (:poison arena-key) arena)))

(defn add-players
  "place players around the arena and return a new arean"
  [players arena]
  (reduce replacer arena players))
;; ----------------------------------
;; END ITEM DROP
;; ----------------------------------

;; ----------------------------------
;; ARENA GENERATION
;; ----------------------------------

(defn empty-arena
  "returns an empty arena"
  [dimx dimy]
  (vec (repeat dimx (vec (repeat dimy (arena-icon :open))))))

(defn new-arena
  "compose all arena building functions to make a fresh new arena"
  [{:keys [dimx dimy food-freq block-freq poison-freq] :as config}]
  (let [arena (empty-arena dimx dimy)]
    ((apply comp (map (fn [item-func item-frequency]
                        (partial item-func item-frequency config))
                      [poison food blocks]
                      [poison-freq food-freq block-freq])) arena)))
;; ----------------------------------
;; END ARENA GENERATION
;; ----------------------------------

;; Example Arena Configurations
;; food-freq, block-freq, and poison-freq represent percentages and will scale
;; with the arena dimensions
(def small-arena {:dimx 20
                  :dimy 20
                  :food-freq 10
                  :block-freq 10
                  :poison-freq 4})

(def large-arena {:dimx 50
                  :dimy 50
                  :food-freq 10
                  :block-freq 10
                  :poison-freq 3})
