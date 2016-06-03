(ns battlebots.game
  (:require [battlebots.arena :as arena]))

(defn add-players
  "place players around the arena and return a new arean"
  [players arena]
  (reduce arena/replacer arena players))
