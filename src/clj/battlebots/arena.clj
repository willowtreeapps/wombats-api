(ns battlebots.arena
  (:require [battlebots.constants.arena :refer [arena-key]]
            [battlebots.utils.arena :refer :all]))

(defn arena-icon
  [key]
  (:display (key arena-key)))

;; ----------------------------------
;; MAP GENERATION HELPERS
;; ----------------------------------

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

(defn- border
  "places block walls contiguously along the border of the arena"
  ;; arenas are vectors (seqs?) of columns -not vectors of rows
  ;; columns are not consistently vectors -they can be seqs
  [{:keys [dimx dimy border]} arena]
  (if border
    (let [block (:block arena-key)
          vwall (repeat dimy block)
          xform (map-indexed (fn [x column]
                               (if (#{0 (dec dimx)} x)
                                 vwall
                                 (-> (vec column)
                                     (assoc-in [0] block)
                                     (assoc-in [(dec dimy)] block)))))]
      (vec (sequence xform arena)))
    arena))
;; ----------------------------------
;; END MAP GENERATION HELPERS
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

(defn new-arena
  "compose all arena building functions to make a fresh new arena"
  [{:keys [dimx dimy food-freq block-freq poison-freq] :as config}]
  (let [arena (empty-arena dimx dimy)]
    ((apply comp (partial border config)
            (map (fn [item-func item-frequency]
                   (partial item-func item-frequency config))
                 [poison food blocks]
                 [poison-freq food-freq block-freq])) arena)))
;; ----------------------------------
;; END ARENA GENERATION
;; ----------------------------------

(defn test-draw-line
  [x1 y1 x2 y2]
  (let [arena (empty-arena (+ 5 (max x1 x2)) (+ 5 (max y1 y2)))
        line (draw-line x1 y1 x2 y2)
        res-arena (reduce (fn [a p] (update-cell a p {:display "*"})) arena line)]
    res-arena))
