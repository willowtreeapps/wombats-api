(ns battlebots.game.bot.helpers)

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

  {:1 {:open   [{:match o :coords [1 3]}
                {:match o :coords [1 4]}
                {:match o :coords [1 5]}]
       :food   [{:match f :coords [2 3]}
                {:match f :coords [3 3]}
                {:match f :coords [3 4]}
                {:match f :coords [3 5]}]
       :block  [{:match b :coords [2 5]}]}
   :2 {:player [{:match b1 :coords [0 2]}]
       :open   [{:match o :coords [1 2]}
                {:match o :coords [2 2]}
                {:match o :coords [4 2]}
                {:match o :coords [0 3]}
                {:match o :coords [0 4]}
                {:match o :coords [0 5]}
                {:match o :coords [0 6]}
                {:match o :coords [1 6]}]
       :food   [{:match f :coords [3 2]}
                {:match f :coords [4 3]}
                {:match f :coords [3 6]}]
       :ai     [{:match ai :coords [4 4]}]
       :block  [{:match b :coords [4 5]}
                {:match b :coords [2 6]}
                {:match b :coords [4 6]}]}}"
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
  (let [tracker (atom {:x -1 :y -1})]
    (:coords (reduce (fn [match row]
                       (swap! tracker assoc :x -1 :y (inc (:y @tracker)))
                       (reduce (fn [match cell]
                                 (swap! tracker assoc :x (inc (:x @tracker)))
                                 (cond
                                  match match
                                  (= item cell) {:match cell
                                                 :coords [(:x @tracker) (:y @tracker)]}
                                  :else match)) match row)) nil arena))))

(defn scan-for
  "Returns a colletion of matches. Each match must return true when passed to a given predicate

  Note: Only use this if you are going to perform a scan and only care about one type
        or you want to perform an advanced arena query (i.e. all cells that are ai and have
        more than 20 energy). Otherwise it is more performant to use sort arena once and
        then operate on the results.

  ex: o  o  o
      b1 o  o
      o  o  b2

  returns: [{:match b1
             :coords [0 1]}
            {:match b2
             :coords [2 2]]

  when the predicate is is-player?"
  [pred arena]
  (let [tracker (atom {:x -1 :y -1})]
    (reduce (fn [matches row]
              (swap! tracker assoc :x -1 :y (inc (:y @tracker)))
              (reduce (fn [matches cell]
                        (swap! tracker assoc :x (inc (:x @tracker)))
                        (if (pred cell)
                          (conj matches {:match cell
                                         :coords [(:x @tracker) (:y @tracker)]})
                          matches)) matches row)) [] arena)))

(defn sort-arena
  "Returns a sorted arena.

  ex: [[o  b  b]
       [o  f  f]
       [b1 o  o]]

  returns: {:open   [{:match o :coords [0 0]}
                     {:match o :coords [0 1]}
                     {:match o :coords [1 2]}
                     {:match o :coords [2 2]}]
            :block  [{:match b :coords [1 0]}
                     {:match b :coords [2 0]}]
            :food   [{:match f :coords [1 1]}
                     {:match f :coords [2 1]}]
            :player [{:match b1 :coords [0 2]}]}"
  [arena]
  (let [tracker (atom {:x -1 :y -1})]
    (reduce (fn [item-map row]
              (swap! tracker assoc :x -1 :y (inc (:y @tracker)))
              (reduce
               (fn [item-map cell]
                 (swap! tracker assoc :x (inc (:x @tracker)))
                 (let [type-of-cell (keyword (or (:type cell) "none"))]
                   (assoc item-map type-of-cell
                          (conj (or (type-of-cell item-map) []) {:match cell
                                                                 :coords [(:x @tracker)
                                                                          (:y @tracker)]}))))
               item-map row))
            {} arena)))

(defn draw-line
  "Draw a line from x1,y1 to x2,y2 using Bresenham's Algorithm

  TODO: Add tests
  "
  [from to]
  (let [[x1 y1] from
        [x2 y2] to
        dist-x (Math/abs (- x1 x2))
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
