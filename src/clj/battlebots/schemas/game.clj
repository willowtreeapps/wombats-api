(ns battlebots.schema.game
  (:require [schema.core :as s]
            [battlebots.schema.round :refer [Round]]))

(s/defschema Game
  "game schema"
  {:id s/Int
   :rounds [Round]})
