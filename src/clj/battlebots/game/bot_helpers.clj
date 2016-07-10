(ns battlebots.game.bot-helpers)

(defn scan-for
  "Returns a colletion of matches. Each match must return true when passed to a given predicate

  Note: Only use this if you are going to perform a scan and only care about one type
        or you want to perform an advanced arena query (i.e. all cells that are ai and have
        more than 20 energy). Otherwise it is more performant to use sort arena once and
        then operate on the results.

  ex: o  o  o
      b1 o  o
      o  o  b2

  returns: [{:match b1
             :coords [0 1]}
            {:match b2
             :coords [2 2]]

  when the predicate is is-player?"
  [pred arena]
  (let [tracker (atom {:x -1 :y -1})]
    (reduce (fn [matches row]
              (swap! tracker assoc :x -1 :y (inc (:y @tracker)))
              (reduce (fn [matches cell]
                        (swap! tracker assoc :x (inc (:x @tracker)))
                        (if (pred cell)
                          (conj matches {:match cell
                                         :coords [(:x @tracker) (:y @tracker)]})
                          matches)) matches row)) [] arena)))

(defn sort-arena
  "Returns a sorted arena.

  ex: [[o  b  b]
       [o  f  f]
       [b1 o  o]]

  returns: {:open   [{:match o :coords [0 0]}
                     {:match o :coords [0 1]}
                     {:match o :coords [1 2]}
                     {:match o :coords [2 2]}]
            :block  [{:match b :coords [1 0]}
                     {:match b :coords [2 0]}]
            :food   [{:match f :coords [1 1]}
                     {:match f :coords [2 1]}]
            :player [{:match b1 :coords [0 2]}]}"
  [arena]
  (let [tracker (atom {:x -1 :y -1})]
    (reduce (fn [item-map row]
              (swap! tracker assoc :x -1 :y (inc (:y @tracker)))
              (reduce
               (fn [item-map cell]
                 (swap! tracker assoc :x (inc (:x @tracker)))
                 (let [type-of-cell (keyword (or (:type cell) "none"))]
                   (assoc item-map type-of-cell
                          (conj (or (type-of-cell item-map) []) {:match cell
                                                                 :coords [(:x @tracker)
                                                                          (:y @tracker)]}))))
               item-map row))
            {} arena)))
