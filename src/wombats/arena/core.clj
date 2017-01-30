(ns wombats.arena.core
  (:require [wombats.arena.utils :as a-utils]))

;; Cell structure
;;
;; {:contents {:type keyword}
;;  :meta [metadata]}

(defn- generate-random-coords
  "generates random coordinates from a given dimension set"
  [[x y]]
  [(rand-int x)
   (rand-int y)])

(defn- find-random-open-space
  "returns the coordinates for a random open space in a given arena"
  [arena]
  (let [arena-dimensions (a-utils/get-arena-dimensions arena)]
    (loop [coords nil]
      (if (and coords (a-utils/pos-open? coords arena))
        coords
        (recur (generate-random-coords arena-dimensions))))))

(defn- replacer
  "replaces an empty cell with a value in a given arena"
  [arena item]
  (a-utils/update-cell-contents arena
                                (find-random-open-space arena)
                                item))

(defn- sprinkle
  "sprinkles given item into an arena"
  [arena item amount]
  (reduce replacer arena (repeat amount item)))

(defn- add-to-arena
  [{:keys [config arena] :as arena-map} item amount]
  (assoc arena-map
         :arena
         (sprinkle arena item amount)))

(defn- add-perimeter
  "Places block walls contiguously along the border of the arena"
  [{:keys [config arena] :as arena-map}]
  (let [{dimx :arena/width
         dimy :arena/height} config
        wall {:type :wood-barrier}
        xform (map-indexed (fn [y row]
                             (if (#{0 (dec dimy)} y)
                               (vec (map #(assoc % :contents wall) row))
                               (-> (vec row)
                                   (assoc-in [0 :contents] wall)
                                   (assoc-in [(dec dimx) :contents] wall)))))]
    (assoc arena-map
           :arena
           (vec (sequence xform arena)))))

(defn- generate-empty-arena
  "creates empty arena"
  [{:keys [config] :as arena-map}]
  (let [{dimx :arena/width
         dimy :arena/height} config]
    (assoc arena-map
           :arena
           (vec (repeat dimy
                        (vec (repeat dimx
                                     {:contents {:type :open}
                                      :meta []})))))))

(defn generate-arena
  [{:keys [:arena/food
           :arena/poison
           :arena/zakano] :as arena-config}]
  (:arena
   (cond-> {:config arena-config
            :arena nil}
     true (generate-empty-arena)
     (:arena/perimeter arena-config) (add-perimeter)
     true (add-to-arena {:type :food} food)
     true (add-to-arena {:type :poison} poison)
     true (add-to-arena {:type :zakano} zakano))))
