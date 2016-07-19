(ns battlebots.schemas.game
  (:require [schema.core :as s]
            [battlebots.schemas.frame :refer [Frame]]
            [battlebots.schemas.player :refer [Player]]))

(s/defschema Game
  "game schema"
  {:_id s/Int
   :frames [Frame]
   :players [Player]
   :state s/Str})
