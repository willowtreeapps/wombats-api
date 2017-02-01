(ns wombats.game.occlusion
  (:require [wombats.game.utils :refer [draw-line]]
            [wombats.arena.utils :as au]))

(defn- caclulate-ray-occlusion
  [ray non-transparent]
  (:occluded
   (reduce
    (fn [{:keys [occluded found-occluded?] :as memo} coords]
      (if found-occluded?
        (merge memo {:occluded (conj occluded coords)})
        (if (contains? non-transparent coords)
          (merge memo {:found-occluded? true})
          memo)))
    {:occluded '()
     :found-occluded? false} ray)))

(defn- calculate-occluded-coords
  [perimeter non-transparent start-pos]
  (let [rays (map #(draw-line start-pos %) perimeter)]
    (set (apply concat (map #(caclulate-ray-occlusion % non-transparent) rays)))))

(defn- calculate-perimeter
  [arena]
  (let [[x y] (au/get-arena-dimensions arena)
        top (map (fn [pos] [pos 0]) (range 0 x))
        bottom (map (fn [pos] [pos (- y 1)]) (range 0 x))
        left (map (fn [pos] [0 pos]) (range 1 (- y 1)))
        right (map (fn [pos] [(- x 1) pos]) (range 1 (- y 1)))]
    (concat top right bottom left)))

(defn- get-non-transparent-types
  [arena-config]
  ;; TODO Move to config
  #{:wood-barrier
    :steel-barrier})

(defn- is-non-transparent-cell?
  "Determines if a cell is transparent"
  [cell non-transparent-types]
  ;; TODO add smoke check here
  (contains? non-transparent-types (get-in cell [:contents :type])))

(defn- get-non-transparent-set
  "Returns a set of coordinates that cause occlusion"
  [arena arena-config]
  (let [non-transparents (get-non-transparent-types arena-config)]
    (set
     (filter #(not (nil? %))
             (apply concat
                    (map-indexed
                     (fn [r-idx row]
                       (map-indexed
                        (fn [c-idx cell]
                          (when (is-non-transparent-cell? cell non-transparents)
                            [c-idx r-idx]))
                        row))
                     arena))))))

(defn get-occluded-arena
  "Returns an arena copy with occluded arena cells"
  [arena start-pos arena-config decision-maker-type]

  (let [non-transparent-cells (get-non-transparent-set arena arena-config)
        perimeter (calculate-perimeter arena)
        occluded-coords (calculate-occluded-coords perimeter
                                                   non-transparent-cells
                                                   start-pos)]

    (map-indexed
     (fn [y-idx row]
       (map-indexed
        (fn [x-idx cell]
          (if (contains? occluded-coords [x-idx y-idx])
            {:contents {:type :fog}}
            cell))
        row))
     arena)))
