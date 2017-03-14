(ns wombats.arena.core
  (:require [wombats.arena.utils :as a-utils]
            [wombats.game.utils :as g-utils]))

(defn- add-to-arena
  "Adds item(s) randomly to an arena"
  [{:keys [config arena] :as arena-map} item amount]
  (assoc arena-map
         :arena
         (a-utils/sprinkle arena item amount)))

(defn- add-perimeter
  "Places block walls contiguously along the border of the arena"
  [{:keys [config arena] :as arena-map}]
  (if (:arena/perimeter config)
    (let [{dimx :arena/width
           dimy :arena/height
           wood-wall-hp :arena/wood-wall-hp} config
          wall (merge (:wood-barrier a-utils/arena-items)
                      {:hp wood-wall-hp})
          xform (map-indexed (fn [y row]
                               (if (#{0 (dec dimy)} y)
                                 (vec (map #(assoc % 
                                                   :contents 
                                                   (a-utils/ensure-uuid wall)) 
                                           row))
                                 (-> (vec row)
                                     (assoc-in [0 :contents] 
                                               (a-utils/ensure-uuid wall))
                                     (assoc-in [(dec dimx) :contents] 
                                               (a-utils/ensure-uuid wall))))))]
      (assoc arena-map
             :arena
             (vec (sequence xform arena))))
    arena-map))

(defn- generate-empty-arena
  "creates empty arena"
  [{:keys [config] :as arena-map}]
  (let [{dimx :arena/width
         dimy :arena/height} config
        open-space (:open a-utils/arena-items)
        contents (a-utils/ensure-uuid open-space)]
    (assoc arena-map
           :arena
           (vec (repeat dimy
                        (vec (repeatedly dimx
                                         (fn []
                                           {:contents contents
                                            :meta []}))))))))

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
          :wrap (g-utils/wrap-coords coordinates' dimensions))
        coordinates'))))


(defn- block?
  [arena next-cell]
  (let [cell-contents (g-utils/get-content-at-coords next-cell arena)]
    (contains? #{:wood-barrier
                 :steel-barrier}
               (:type cell-contents))))

(defn- wall-adjacent?
  "Is the given coordinate pair adjacent to a wall?"
  [coordinates arena dimensions]
  (some (fn [d]
          (let [next-cell (move coordinates d dimensions)]
            (when (block? arena next-cell)
              [d next-cell])))
        (list :n :e :s :w)))

(defn- place-wall
  "place a single contiguous block wall of max length 
  l starting at coordinates start in direction d"
  [starting-cell
   walls-remaining
   direction
   arena-dimensions
   arena
   wall]
  {:pre [((complement neg?) walls-remaining)
         (#{:n :e :s :w} direction)]}
  (loop [arena arena
         l' 0
         head starting-cell
         anchor-count 0]
    (let [anchor-count (+ anchor-count 
                          (if (wall-adjacent? starting-cell
                                              arena
                                              arena-dimensions)
                            1 0))]
      (if (and (< l' walls-remaining)
               (a-utils/pos-open? head arena) (< anchor-count 2))
        (let [arena' (a-utils/update-cell-contents arena
                                                   head
                                                   (a-utils/ensure-uuid wall))
              head' (move head
                          direction
                          arena-dimensions
                          :oob
                          :return)]
          (recur arena' (inc l') head' anchor-count))
        [l' arena]))))

(defn- get-wall-contents
  [wall-type {:keys [:arena/wood-wall-hp
                     :arena/steel-wall-hp]}]
  (case wall-type
    :arena/wood-walls (merge (:wood-barrier a-utils/arena-items)
                             {:hp wood-wall-hp})
    :arena/steel-walls (merge (:steel-barrier a-utils/arena-items)
                              {:hp steel-wall-hp})))

(defn- place-walls
  "places walls in an organized structures around the arena"
  [{:keys [arena config] :as arena-map}
   wall-type]

  (let [wall-count (wall-type config)
        arena-dimensions (a-utils/get-arena-dimensions arena)
        arena-update
        (loop [arena arena
               walls-remaining (int wall-count)
               tries 50]
          (cond
            (and (pos? tries)
                 (pos? walls-remaining))
            (let [starting-cell (a-utils/find-random-open-space arena)
                  direction (rand-nth [:n :s :e :w])
                  [walls-placed arena'] (place-wall starting-cell
                                                    walls-remaining
                                                    direction
                                                    arena-dimensions
                                                    arena

                                                    (get-wall-contents wall-type config))]
              (recur arena'
                     (- walls-remaining walls-placed)
                     (dec tries)))

            (pos? tries)
            arena

            :else
            arena))]
    (assoc arena-map :arena arena-update)))

(defn generate-arena
  [{:keys [:arena/food
           :arena/poison
           :arena/zakano
           :arena/zakano-hp] :as arena-config}]

  (-> {:config arena-config
       :arena nil}
      (generate-empty-arena)
      (add-perimeter)
      (place-walls :arena/wood-walls)
      (place-walls :arena/steel-walls)
      (add-to-arena (:food a-utils/arena-items) food)
      (add-to-arena (:poison a-utils/arena-items) poison)
      (add-to-arena (merge (:zakano a-utils/arena-items)
                           {:orientation (g-utils/rand-orientation)
                            :hp zakano-hp}) zakano)
      (:arena)))
