(ns battlebots.constants.arena)

(def arena-key {:open   {:type "open"
                         :display " "
                         :transparent true
                         :can-occupy true}
                :ai     {:type "ai"
                         :display "@"
                         :transparent true
                         :energy 20
                         :can-occupy false
                         :destructible true}
                :block  {:type "block"
                         :display "X"
                         :transparent false
                         :can-occupy false
                         :destructible true
                         :energy 20}
                :food   {:type "food"
                         :display "+"
                         :transparent true
                         :can-occupy true
                         :destructible false}
                :poison {:type "poison"
                         :display "-"
                         :transparent true
                         :can-occupy true
                         :destructible false}
                :fog    {:type "fog"
                         :display "?"
                         :transparent false
                         :can-occupy true
                         :destructible false}
                :shoot  {:type "shoot"
                         :display "!"
                         :transparent true
                         :can-occupy true
                         :destructible false
                         :volatile true}})

;; Example Arena Configurations
;; food-freq, block-freq, and poison-freq represent percentages and will scale
;; with the arena dimensions
(def small-arena {:dimx 20
                  :dimy 20
                  :border {:tunnel false}
                  :ai-freq 3
                  :food-freq 10
                  :block-freq 10
                  :poison-freq 4})

(def large-arena {:dimx 50
                  :dimy 50
                  :border {:tunnel false}
                  :ai-freq 3
                  :food-freq 10
                  :block-freq 10
                  :poison-freq 3})

(defn can-occupy?
  [key]
  (:can-occupy (get arena-key (keyword key) {:can-occupy false})))

(defn destructible?
  [key]
  (:destructible (get arena-key (keyword key) {:destructible false})))
