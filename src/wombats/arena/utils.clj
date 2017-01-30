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

;; Random

(defn ensure-uuid
  [{:keys [uuid] :as item}]
  (if uuid
    item
    (assoc item :uuid (str (java.util.UUID/randomUUID)))))

(defn- print-cell
  [cell]
  (print (get-in cell [:contents :type])))

(defn- print-row
  [row]
  (print "\n")
  (doall (map print-cell row)))

(defn print-arena
  "Pretty prints an arena"
  [arena {:keys [:arena/height
                 :arena/width] :as arena-config}]

  (println "\n--------------------------------")
  (doall (map print-row arena))
  (println "\n\n--------------------------------"))

;; Modifiers. Probably belong somewhere else

(defn update-cell
  "Updates a cells contents and metadata"
  [arena coords item]
  (if (coords-inbounds? coords arena)
    (assoc-in arena coords item)
    arena))

(defn update-cell-contents
  "Update a cells contents"
  [arena coords new-contents]
  (if (coords-inbounds? coords arena)
    (assoc-in arena (conj coords :contents) new-contents)
    arena))

(defn update-cell-metadata
  "Update a cells metadata"
  [arena coords new-meta]
  (if (coords-inbounds? coords arena)
    (assoc-in arena (conj coords :meta) new-meta)
    arena))
