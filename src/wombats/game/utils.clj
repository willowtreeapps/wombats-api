(ns wombats.game.utils
  (:require [wombats.arena.utils :as au]))

(defonce decision-maker-state {:code nil
                               :command nil
                               :error nil
                               :saved-state {}})

(defonce orientations [:n :e :s :w])

(defn modify-orientation
  [current-orientation modifier]
  (let [current-idx (.indexOf orientations current-orientation)]
    (if (not= current-idx -1)
      (condp = modifier
          :right (get orientations (mod (inc current-idx) 4))
          :left (get orientations (mod (dec current-idx) 4))
          :about-face (get orientations (mod (+ 2 current-idx) 4))
          current-orientation)
      current-orientation)))

(defn rand-orientation
  "Returns a random orientation"
  []
  (rand-nth (seq orientations)))

(defn get-item-at-coords
  [[x y] arena]
  (get-in arena [y x]))

(defn get-content-at-coords
  [coords arena]
  (:contents (get-item-at-coords coords arena)))

(defn get-item-type
  "Returns the type of an arena item"
  [item]
  (get-in item [:contents :type]))

(defn get-uuids
  "Gets all the uuids in a given arena that match a specified :type"
  [arena item-type]
  (reduce (fn [uuids item]
            (if (= (get-in item [:contents :type])
                   item-type)
              (conj uuids (get-in item [:contents :uuid]))
              uuids))
          [] (flatten arena)))

(defn is-player?
  [contents]
  (= (:type contents) :wombat))

(defn get-item-coords
  "Given an arena and uuid, lookup position of uuid"
  [arena uuid]
  (first
   (for [[y row] (map-indexed vector arena)
         [x val] (map-indexed vector row)
         :when (= (get-in val [:contents :uuid]) uuid)]
     [x y])))

(defn get-item-and-coords
  "Given an arena and uuid, lookup position of uuid and return its contents"
  [arena uuid]
  (first
   (for [[y row] (map-indexed vector arena)
         [x val] (map-indexed vector row)
         :when (= (get-in val [:contents :uuid]) uuid)]
     {:coords [x y]
      :item val})))

(defn wrap-coords
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

(defn orientation-to-direction
  [orientation]
  (condp = orientation
    :n 1
    :e 3
    :s 5
    :w 7
    nil))

(defn draw-line-from-point
  "Draws a line from on point to another in a given arena"
  [arena pos direction dist]
  (let [arena-dimensions (au/get-arena-dimensions arena)]
    (vec (map (fn [idx]
                (vec (wrap-coords
                      (map (fn [fnc dim] (fnc dim))
                           (directional-functions direction idx) pos)
                      arena-dimensions)))
              (range 1 (inc dist))))))

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
