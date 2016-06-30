(ns battlebots.constants.arena)

(def arena-key {:open   {:type "open"
                         :display " "
                         :transparent true}
                :bot    {:type "bot"
                         :display "@"
                         :transparent false}
                :block  {:type "block"
                         :display "X"
                         :transparent false
                         :energy 20}
                :food   {:type "food"
                         :display "+"
                         :transparent true}
                :poison {:type "poison"
                         :display "-"
                         :transparent true}
                :fog    {:type "fog"
                         :display "?"
                         :transparent false}})

;; Example Arena Configurations
;; food-freq, block-freq, and poison-freq represent percentages and will scale
;; with the arena dimensions
(def small-arena {:dimx 20
                  :dimy 20
                  :border {:tunnel false}
                  :food-freq 10
                  :block-freq 10
                  :poison-freq 4})

(def large-arena {:dimx 50
                  :dimy 50
                  :border {:tunnel false}
                  :food-freq 10
                  :block-freq 10
                  :poison-freq 3})
