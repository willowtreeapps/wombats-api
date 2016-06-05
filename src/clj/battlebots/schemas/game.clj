(ns battlebots.schemas.game
  (:require [schema.core :as s]
            [battlebots.schemas.round :refer [Round]]
            [battlebots.schemas.player :refer [Player]]))

(s/defschema Game
  "game schema"
  {:_id s/Int
   :rounds [Round]
   :players [Player]
   :state s/Str})
