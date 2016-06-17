(ns battlebots.constants.arena)

(def arena-key {:open   {:type "open"
                         :display " "
                         :transparent true}
                :bot    {:type "bot"
                         :display "@"
                         :transparent false}
                :block  {:type "block"
                         :display "X"
                         :transparent false}
                :food   {:type "food"
                         :display "+"
                         :transparent false}
                :poison {:type "poison"
                         :display "-"
                         :transparent false}})
