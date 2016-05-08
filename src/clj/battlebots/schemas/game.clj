(ns battlebots.schema.game
  (:require [schema.core :as s]
            [battlebots.schema.round :refer [Round]]
            [battlebots.schema.player :refer [Player]]))

(s/defschema Game
  "game schema"
  {:_id s/Int
   :rounds [Round]
   :players [Player]})
