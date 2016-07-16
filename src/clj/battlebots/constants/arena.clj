(ns battlebots.constants.arena)

(def arena-key {:open   {:type "open"
                         :display " "
                         :transparent true}
                :ai     {:type "ai"
                         :display "@"
                         :transparent true
                         :energy 20}
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
                         :transparent false}
                :steel  {:type "steel"
                         :display "&"
                         :transparent false}})

(def move-settings {:can-occupy #{:open
                                  :food
                                  :poison}
                    :effects {:open {}
                              :food {:energy #(+ % 10)}
                              :poision {:energy #(- % 5)}}})

(def shot-settings {:can-occupy #{:open
                                  :ai
                                  :block
                                  :food
                                  :poison
                                  :fog
                                  :shoot}
                    :destructible #{:ai
                                    :block}
                    :distance 10})

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
  [key settings]
  (contains? (get settings :can-occupy) (keyword key)))

(defn determine-effects
  [key settings]
  (get-in settings [:effects (keyword key)]))

(defn destructible?
  [key settings]
  (contains? (get settings :destructible) (keyword key)))
