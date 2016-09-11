(ns wombats-api.arena.occlusion
  (:require [wombats-api.game.bot.helpers :refer [draw-line sort-arena]]
            [wombats-api.constants.arena :refer [arena-key]]
            [wombats-api.arena.utils :as au]))



(def ^:private slopes [[1 0] [-1 0] [0 1] [0 -1]])

#_(defn- normalize-slope
  [[x y]]
  (if (zero? y)
    [(/ x (Math/abs x)) y]
    (let [length (Math/sqrt (+ (* x x) (* y y)))]
      [(/ x length) (/ y length)])))

#_(defn- corners
  [[x y]]
  (let [[nx ny] (normalize-slope [x y])
        negx (- nx)
        negy (- ny)]
    [[nx ny] [negx ny] [nx negy] [negx negy]]))

#_(defn- angles
  [view-dist]
  (concat slopes
          (mapcat #(corners [% view-dist]) (range 1 (inc view-dist)))
          (mapcat #(corners [view-dist %]) (range 1 view-dist))))

#_(defn- blocked-pos?
  [arena coords]
  (let [cell (au/get-item coords arena)]
    (or
     (opaque-md? cell)
     (not (:transparent cell)))))

#_(defn- fog-of-war
  [arena coords]
  (if (au/get-item coords arena)
    (au/update-cell arena coords (:fog arena-key))
    arena))

#_(defn- finalize-pos
  [arena pos]
  [pos (blocked-pos? arena pos)])

#_(defn- new-pos
  [ray-length rxy xy]
  (int (Math/round (+ rxy (* xy (double ray-length))))))

#_(defn- parse-pos
  [arena view-dist ray-length [rx ry] [x y :as ang]]
  (if (zero? x)
    (finalize-pos arena [(int rx)
                         (int (+ ry (* ray-length x)))])
    (finalize-pos arena [(new-pos ray-length rx x)
                         (new-pos ray-length ry y)])))

#_(defn- blocked-pos
  [arena view-dist player-pos]
  ;; TODO There is a bug where player-pos is sometimes not passed to
  ;; blocked-pos. Find that edge case and remove the if statement.
  (if (not player-pos)
    #{}
    (let [angs (angles view-dist)]
      (:blocked-pos
       (reduce
        (fn [bmap ray-length]
          (reduce
           (fn [blocked-map ang]
             (let [ang-blocked? (contains? (:blocked-angs blocked-map) ang)
                   [pos pos-blocked?] (parse-pos arena view-dist ray-length
                                                 player-pos ang)
                   blocker? (contains? (:blockers blocked-map) pos)]
               (-> blocked-map
                   (update-in [:blockers]
                              #(if (and pos-blocked?
                                        (not ang-blocked?))
                                 (conj % pos) %))
                   (update-in [:blocked-angs]
                              #(if pos-blocked?
                                 (conj % ang) %))
                   (update-in [:blocked-pos]
                              #(if (and ang-blocked?
                                        (not blocker?))
                                 (conj % pos) %)))))
           bmap angs))
        {:blocked-angs #{}
         :blocked-pos #{}
         :blockers #{}}
        (range 1 (inc view-dist)))))))

#_(defn occluded-arena
  "Only pass the limited arena that the user can see,
   this fn does not alter the size of the arena passed."
  [arena player-pos]
  (let [blocked-pos (blocked-pos arena (count arena) player-pos)]
    (reduce fog-of-war arena blocked-pos)))

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
  (let [sorted-arena (sort-arena arena)
        non-transparent (filter-non-transparent arena)
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
