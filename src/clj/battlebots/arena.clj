(ns battlebots.arena)

(def arena-key {:open " "
                :bot "@"
                :block "X"})

(defn arena
  [dimx dimy]
  (vec (replicate (vec (replicate (:open arena-key) dimy)) dimx)))

