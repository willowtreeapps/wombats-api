(ns wombats.schemas.game
  (:require [schema.core :as s]
            [wombats.schemas.frame :refer [Frame]]
            [wombats.schemas.player :refer [Player]]))

(s/defschema Game
  "game schema"
  {:_id s/Int
   :frames [Frame]
   :players [Player]
   :state s/Str})
