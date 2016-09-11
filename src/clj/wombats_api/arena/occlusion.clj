(ns wombats-api.arena.occlusion
  (:require [wombats-api.game.bot.helpers :refer [draw-line]]
            [wombats-api.constants.arena :refer [arena-key]]
            [wombats-api.arena.utils :as au]))

(defn- is-opaque-md?
  [cell]
  (some (fn [kv]
          (let [v (get kv 1)]
            (= :smokescreen (get v :type)))) (:md cell)))

(defn- is-non-transparent-cell?
  [cell non-transparent-types]
  (or (contains? non-transparent-types (:type cell))
      (is-opaque-md? cell)))

(defn- get-non-transparent-types
  []
  (set
   (map #(name %)
        (keys (filter (fn [[key-name details]]
                        (not (:transparent details))) arena-key)))))

(defn- filter-non-transparent
  [arena]
  (let [non-transparent-types (get-non-transparent-types)]
    (set (filter #(not (nil? %)) (apply concat
                                         (map-indexed
                                          (fn [r-idx row]
                                            (map-indexed
                                             (fn [c-idx cell]
                                               (when (is-non-transparent-cell? cell non-transparent-types)
                                                 [c-idx r-idx]))
                                             row))
                                          arena))))))

(defn- calculate-perimeter
  [arena]
  (let [[x y] (au/get-arena-dimensions arena)
        top (map (fn [pos] [pos 0]) (range 0 x))
        bottom (map (fn [pos] [pos (- y 1)]) (range 0 x))
        left (map (fn [pos] [0 pos]) (range 1 (- y 1)))
        right (map (fn [pos] [(- x 1) pos]) (range 1 (- y 1)))]
    (concat top right bottom left)))

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

(defn occluded-arena
  [arena start-pos]
  (let [non-transparent (filter-non-transparent arena)
        perimeter (calculate-perimeter arena)
        occluded-coords (calculate-occluded-coords perimeter
                                                   non-transparent
                                                   start-pos)]
    (map-indexed
     (fn [r-idx row]
       (map-indexed
        (fn [c-idx cell]
          (if (contains? occluded-coords [c-idx r-idx])
            (:fog arena-key)
            cell))
        row))
     arena)))

(comment
  ;; eval (C-x C-e) each line to see the occluded-arena work
  (require '[clojure.edn :as edn])
  (require '[clojure.java.io :as io])
  (require '[wombats-api.arena.utils :refer [pprint-arena]])
  (def t-arena (edn/read-string (slurp "env/dev/resources/test_arena.edn")))
  (def o-arena (occluded-arena t-arena [1 3]))
  (pprint-arena t-arena)
  (pprint-arena o-arena)
)
