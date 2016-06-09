(ns battlebots.arena)

;; map of possible arena values
(def arena-key {:open   {:type "open"
                         :display " "}
                :bot    {:type "bot"
                         :display "@"}
                :block  {:type "block"
                         :display "X"}
                :food   {:type "food"
                         :display "+"}
                :poison {:type "poison"
                         :display "-"}})

;; ----------------------------------
;; MAP GENERATION HELPERS
;; ----------------------------------

(defn get-arena-dimensions
  "returns the dimensions of a given arena"
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
  "updates a cell with a specified value"
  [[x y] new-value arena]
  (let [row (get arena x)]
    (assoc arena x (assoc row y new-value))))

(defn replacer
  "replaces an empty cell with a value in a given arena"
  [arena item]
  (update-cell (find-random-open-space arena) item arena))

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
  (vec (repeat dimx (vec (repeat dimy (:open arena-key))))))

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
