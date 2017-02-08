(ns wombats.arena.utils)

;; Constants

(def arena-items {:open {:type :open}
                  :wood-barrier {:type :wood-barrier}
                  :steel-barrier {:type :steel-barrier}
                  :wombat {:type :wombat}
                  :zakano {:type :zakano}
                  :smoke {:type :smoke}
                  :fog {:type :fog}
                  :food {:type :food}
                  :poison {:type :poison}})

;; Accessors

(defn get-arena-dimensions
  "returns the dimensions of a given arena (NOTE: Not 0 based)"
  [arena]
  (let [x ((comp count first) arena)
        y (count arena)]
    [x y]))

(defn pos-open?
  "returns true of false depending if a given coodinate in a given arena is open"
  [[x y] arena]
  (= :open
     (get-in arena [y x :contents :type])))

(defn coords-inbounds?
  [[x y] arena]
  (let [[xdim ydim] (get-arena-dimensions arena)]
    (and (< x xdim)
         (< y ydim))))

(defn ensure-uuid
  [{:keys [uuid] :as item}]
  (if uuid
    item
    (assoc item :uuid (str (java.util.UUID/randomUUID)))))

(defn create-new-contents
  [content-type]
  (ensure-uuid (content-type arena-items)))

(defn- print-cell
  [cell]
  (let [display-values {:wood-barrier "w"
                        :steel-barrier "s"
                        :wombat "W"
                        :zakano "z"
                        :poison "p"
                        :food "f"
                        :open "o"
                        :fog "?"}]
    (print (format
            " %s "
            (-> cell
                (get-in [:contents :type])
                (display-values))))))

(defn- print-row
  [row]
  (print "\n")
  (doall (map print-cell row)))

(defn print-arena
  "Pretty prints an arena"
  [arena]

  (let [[x y] (get-arena-dimensions arena)]

    (println "\n--------------------------------")

    (print (str " " (clojure.string/join "  " (range 0 x))))
    (doall (map print-row arena))
    (println "\n\n--------------------------------")))

;; Modifiers. Probably belong somewhere else

(defn update-cell
  "Updates a cells contents and metadata"
  [arena [x y] item]
  (if (coords-inbounds? [x y] arena)
    (assoc-in arena [y x] item)
    arena))

(defn update-cell-contents
  "Update a cells contents"
  [arena [x y] new-contents]
  (if (coords-inbounds? [x y] arena)
    (assoc-in arena [y x :contents] new-contents)
    arena))

(defn update-cell-metadata
  "Update a cells metadata"
  [arena [x y] new-meta]
  (if (coords-inbounds? [x y] arena)
    (assoc-in arena [y x :meta] new-meta)
    arena))

(defn update-cell-metadata-with
  "updates a cells metadata with a given function"
  [arena [x y] update-fn]
  (if (coords-inbounds? [x y] arena)
    (update-in arena [y x :meta] update-fn)
    arena))

(defn- generate-random-coords
  "generates random coordinates from a given dimension set"
  [[x y]]
  [(rand-int x)
   (rand-int y)])

(defn- find-random-open-space
  "returns the coordinates for a random open space in a given arena"
  [arena]
  (let [arena-dimensions (get-arena-dimensions arena)]
    (loop [coords nil]
      (if (and coords (pos-open? coords arena))
        coords
        (recur (generate-random-coords arena-dimensions))))))

(defn- replacer
  "replaces an empty cell with a value in a given arena"
  [arena item]
  (update-cell-contents arena
                        (find-random-open-space arena)
                        (ensure-uuid item)))

(defn sprinkle
  "sprinkles given item into an arena"
  ([arena item]
   (sprinkle arena item 1))
  ([arena item amount]
   (reduce replacer arena (repeat amount item))))
