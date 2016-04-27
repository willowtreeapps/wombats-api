(ns battlebots.arena)

(def arena-key {:open " "
                :bot "@"
                :block "X"
                :food "+"
                :poison "-"})

(defn empty-arena
  [dimx dimy]
  (vec (replicate dimx (vec (replicate dimy (:open arena-key))))))

(defn blocks
  "sprinkle blocks around the arena argument and return a new arena
  make sure there are no inaccessible areas"
  [arena]
  arena)

(defn food
  "sprinkle food around the arena argument and return a new arena"
  [arena]
  arena)

(defn poison
  "sprinkle poison around the arena argument and return a new arena"
  [arena]
  arena)

(defn new-arena
  "compose all arena building functions to make a fresh new arena"
  [dimx dimy]
  ((comp poison food blocks empty-arena) dimx dimy))



