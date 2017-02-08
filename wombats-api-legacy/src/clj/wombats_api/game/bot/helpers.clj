(ns wombats-api.game.bot.helpers)

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PRIVATE FUNCTIONS
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- last-cell-of-row?
  "Returns true or false indicating if the given cell is the last cell of the given row"
  [cell row arena]
  (let [row-n (get arena row)]
    (= (dec (count row-n)) cell)))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PUBLIC FUNCTIONS
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn calculate-direction-from-origin
  "calculates the direction when supplied two adjacent coords

  Arena

  00 10 20

  01 11 21

  02 12 22

  ex: [1 1] [0 1]
  returns: 1

  ex: [1 1] [2 2]
  returns: 4
  "
  [origin destination]
  (let [[ox oy] origin
        [dx dy] destination
        x (- dx ox)
        y (- dy oy)]
    (condp = [x y]
      [-1 -1] 0
      [0 -1]  1
      [1 -1]  2
      [1 0]   3
      [1 1]   4
      [0 1]   5
      [-1 1]  6
      [-1 0]  7
      nil)))

(defn within-n-spaces
  "Returns a subset of sorted-arena. The subset is determined by the provided radius

  Sample return map when searching at a radius of 2:

    :arena   0  1  2  3  4  5  6
           [[o  o  f  o  o  b  b]  0
            [f  o  f  f  o  b  o]  1
            [b1 o  o  f  o  f  f]  2
            [o  o  f  f  f  b  o]  3
            [o  o  f  f  ai o  f]  4
            [o  o  b  f  b  o  f]  5
            [o  o  b  f  b  o  f]] 6

    :origin [2 4]

    :radius 2

  returns:

  {:1 {:open   [(assoc o :coords [1 3])
                (assoc o :coords [1 4])
                (assoc o :coords [1 5])]
       :food   [(assoc f :coords [2 3])
                (assoc f :coords [3 3])
                (assoc f :coords [3 4])
                (assoc f :coords [3 5])]
       :block  [(assoc b :coords [2 5])]}
   :2 {:player [(assoc b1 :coords [0 2])]
       :open   [(assoc o :coords [1 2])
                (assoc o :coords [2 2])
                (assoc o :coords [4 2])
                (assoc o :coords [0 3])
                (assoc o :coords [0 4])
                (assoc o :coords [0 5])
                (assoc o :coords [0 6])
                (assoc o :coords [1 6])]
       :food   [(assoc f :coords [3 2])
                (assoc f :coords [4 3])
                (assoc f :coords [3 6])]
       :ai     [(assoc ai :coords [4 4])]
       :block  [(assoc b :coords [4 5])
                (assoc b :coords [2 6])
                (assoc b :coords [4 6])]}}"
  [sorted-arena [x y] radius]
  (reduce (fn [found-spaces [space-type space-collection]]
            (reduce (fn [found-spaces {:keys [coords] :as space}]
                     (let [[coord-x coord-y] coords
                            x-delta (Math/abs (- x coord-x))
                            y-delta (Math/abs (- y coord-y))
                            distance ((comp keyword str) (Math/max x-delta y-delta))]
                       (if (and (<= x-delta radius)
                                (<= y-delta radius)
                                (not (= coords [x y])))
                         (assoc-in found-spaces [distance space-type]
                                   (conj (get-in found-spaces [distance space-type] []) space))
                         found-spaces)))
                    found-spaces space-collection)) {} sorted-arena))

(defn get-items-coords
  "Given an item and an arena, get-items-coords will return the coordinates associated
  with the item or nil if no item is found.

  ex:
    :item a1
    :arena o1 f2 o4
           o5 b1 a1
           o2 o3 f1

  returns: [2 1]
  "
  [item arena]
  (loop [row 0
         cell 0]
    (let [current-cell (get-in arena [row cell])]
      (cond
       (= current-cell nil) nil
       (= current-cell item) [cell row]
       :else  (if (last-cell-of-row? cell row arena)
                (recur (inc row) 0)
                (recur row (inc cell)))))))

(defn scan-for
  "Returns a colletion of matches. Each match must return true when passed to a given predicate

  Note: Only use this if you are going to perform a scan and only care about one type
        or you want to perform an advanced arena query (i.e. all cells that are ai and have
        more than 20 hp). Otherwise it is more performant to use sort arena once and
        then operate on the results.

  ex: o  o  o
      b1 o  o
      o  o  b2

  returns: [(assoc b1 :coords [0 1]}
            (assoc b2 :coords [2 2]]

  when the predicate is 'is-player?'"
  [pred arena]
  (loop [row 0
         cell 0
         matches []]
    (let [current-cell (get-in arena [row cell])]
      (cond
       (= current-cell nil) matches
       (pred current-cell) (let [update (conj matches (assoc current-cell :coords [cell row]))]
                             (if (last-cell-of-row? cell row arena)
                               (recur (inc row) 0 update)
                               (recur row (inc cell) update)))
       :else (if (last-cell-of-row? cell row arena)
               (recur (inc row) 0 matches)
               (recur row (inc cell) matches))))))

(defn- arena-by
  ([arena]
   (arena-by arena :type "none"))

  ([arena akey default]
   (apply concat
          (map-indexed
           #(map-indexed
             (fn [idx itm]
               (let [coords [idx %1]]
                 {(keyword (or (get itm akey) default)) [(assoc itm :coords coords)]}))
             %2)
           arena))))

(defn sort-arena
  "Returns a sorted arena.

  ex: [[o  b  b]
       [o  f  f]
       [b1 o  o]]

  returns: {:open   [(assoc o :coords [0 0])
                     (assoc o :coords [0 1])
                     (assoc o :coords [1 2])
                     (assoc o :coords [2 2])]
            :block  [(assoc b :coords [1 0])
                     (assoc b :coords [2 0])]
            :food   [(assoc f :coords [1 1])
                     (assoc f :coords [2 1])]
            :player [(assoc b1 :coords [0 2])]}"
  [arena]
  (apply merge-with into (arena-by arena)))

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
