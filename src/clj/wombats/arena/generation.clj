(ns wombats.arena.generation
  (:require [wombats.constants.arena :refer [arena-key]]
            [clojure.tools.logging :as log]
            [wombats.arena.utils :refer [get-arena-dimensions
                                         update-cell
                                         uuid
                                         wrap-coords]]))


(defn- get-number-of-items
  "reutrns the number of items based off of a given frequency and the total number
  of cells in a given arena"
  [frequency arena]
  (* (/ (or frequency 0) 100) (apply * (get-arena-dimensions arena))))

(defn- pos-open
  "returns true of false depending if a given coodinate in a given arena is open"
  [[x y] arena]
  (= (:open arena-key) (get-in arena [y x])))

(defn- generate-random-coords
  "generates random coordinates from a given dimension set"
  [[x y]]
  [(rand-int x) (rand-int y)])

(defn- find-random-open-space
  "returns the coordinates for a random open space in a given arena"
  [arena]
  (let [arena-dimensions (get-arena-dimensions arena)]
    (loop [dimensions nil]
      (if (and dimensions (pos-open dimensions arena))
        dimensions
        (recur (generate-random-coords arena-dimensions))))))

(defn- replacer
  "replaces an empty cell with a value in a given arena"
  [arena item]
  (update-cell arena
               (find-random-open-space arena)
               (assoc item :uuid (uuid))))

(defn- sprinkle
  "sprinkles given items into an arena"
  [amount item arena]
  (reduce replacer arena (repeat amount item)))

(def ^:private directions #{:n :s :e :w :ne :se :sw :nw})

(let [moves {:n [ 0 -1] :ne [ 1 -1] :e [ 1  0] :se [ 1  1]
             :s [ 0  1] :sw [-1  1] :w [-1  0] :nw [-1 -1]}]
  (defn- move
    "Return new coordinates for a move from coordinates in direction"
    [coordinates direction [m n :as dimensions] & {:keys [oob] :or {oob :wrap}}]
    {:pre [(moves direction)]
     :post [(let [[x y] %] (and (integer? x) (integer? y)))]}
    (let [oob? (fn [[x y]] (not (and (<= 0 x (dec m)) (<= 0 y (dec n)))))
          coordinates' (mapv + coordinates (moves direction))]
      (if (oob? coordinates')
        (case oob
          :return coordinates
          :wrap (wrap-coords coordinates' dimensions))
        coordinates'))))

(letfn [(block? [arena c] (= (:block arena-key) (get-in arena (reverse c))))]
  (defn- wall-adjacent?
    "Is the given coordinate pair adjacent to a wall?"
    [coordinates arena]
    (let [dimensions (get-arena-dimensions arena)]
      (some (fn [d] (let [c (move coordinates d dimensions)]
                     (when (block? arena c) [d c])))
            (list :n :e :s :w)))))

(defn- place-wall
  "place a single contiguous block wall of max length l starting at coordinates start in direction d"
  [start l d arena]
  {:pre [((complement neg?) l) (#{:n :e :s :w} d)]}
  (let [dimensions (get-arena-dimensions arena)]
    (loop [arena arena l' 0 head start anchor-count 0]
      (let [anchor-count (+ anchor-count (if (wall-adjacent? start arena) 1 0))]
        (if (and (< l' l) (pos-open head arena) (< anchor-count 2))
          (let [arena' (update-cell arena head (:block arena-key))
                head' (move head d dimensions :oob :return)]
            (recur arena' (inc l') head' anchor-count))
          [l' arena])))))

(defn- place-walls
  "places walls in an organized structures around the arena"
  [amount arena]
  (loop [arena arena l (int amount) tries 50]
    (log/debugf "%d tries left to place length %d" tries l)
    (cond
      (and (pos? tries) (pos? l))
      (let [start (find-random-open-space arena)
            direction (rand-nth [:n :s :e :w])
            [l' arena'] (place-wall start l direction arena)]
        (log/debugf "Placed wall of length %d" l')
        (recur arena' (- l l') (dec tries)))
      (pos? tries) arena ; done
      :true (do (log/warn "Can't place walls!")
                arena))))

;; ;;;;;;;;;;;;;;;;;;;;;;;;
;; PLACE ITEMS
;; ;;;;;;;;;;;;;;;;;;;;;;;;

(defn- border
  "places block walls contiguously along the border of the arena"
  ;; arenas are vectors (seqs?) of rows -not vectors of columbs
  ;; rows are not consistently vectors -they can be seqs
  [{:keys [dimx dimy border]} arena]
  (if border
    (let [block (:block arena-key)
          hwall (repeat dimx block)
          xform (map-indexed (fn [y row]
                               (if (#{0 (dec dimy)} y)
                                 hwall
                                 (-> (vec row)
                                     (assoc-in [0] block)
                                     (assoc-in [(dec dimx)] block)))))]
      (vec (sequence xform arena)))
    arena))

(defn- blocks
  "sprinkle blocks around the arena and return a new arena
  make sure there are no inaccessible areas"
  [frequency config arena]
  (let [amount (get-number-of-items frequency arena)]
    (place-walls amount arena)))

(defn- food
  "sprinkle food around the arena and return a new arena"
  [frequency config arena]
  (let [amount (get-number-of-items frequency arena)]
    (sprinkle amount (:food arena-key) arena)))

(defn- poison
  "sprinkle poison around the arena and return a new arena"
  [frequency config arena]
  (let [amount (get-number-of-items frequency arena)]
    (sprinkle amount (:poison arena-key) arena)))

(defn- ai
  "sprinkle ai bots around the arena and return a new arena"
  [frequency config arena]
  (let [amount (get-number-of-items frequency arena)]
    (sprinkle amount (:ai arena-key) arena)))

(defn- players
  "place players around the arena and returns a new arena"
  [players arena]
  (reduce replacer arena players))

;; ;;;;;;;;;;;;;;;;;;;;;;
;; END PLACE ITEMS
;; ;;;;;;;;;;;;;;;;;;;;;;

(defn add-players
  "Public interface for adding players to an arena"
  [player-collection arena]
  (players player-collection arena))

(defn empty-arena
  "returns an empty arena"
  [dimx dimy]
  (vec (repeat dimy (vec (repeat dimx (:open arena-key))))))

(defn new-arena
  "compose all arena building functions to make a fresh new arena"
  [{:keys [dimx dimy food-freq block-freq poison-freq ai-freq] :as config}]
  (let [arena (empty-arena dimx dimy)]
    ((apply comp
            (partial border config)
            (map (fn [item-func item-frequency]
                   (partial item-func item-frequency config))
                 [ai poison food blocks]
                 [ai-freq poison-freq food-freq block-freq])) arena)))
