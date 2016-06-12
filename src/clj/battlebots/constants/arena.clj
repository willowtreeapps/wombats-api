(ns battlebots.constants.arena)

;; map of possible arena values
(def arena-key {:open   {:type "open"
                         :display " "}
                :bot    {:type "bot"
                         :display "@"}
                :block  {:type "block"
                         :display "X"}
                :food   {:type "food"
                         :display "+"}
                :poison {:type "poison"
                         :display "-"}})
