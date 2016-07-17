(ns battlebots.schemas.simulation
  (:require [schema.core :as s]))

(s/defschema Simulation
  "simulation schema"
  {:arena [[s/Any]]
   :bot s/Str
   :energy s/Int
   :saved-state s/Any
   :frames s/Int})

(defn is-simulation?
  [simulation]
  (s/validate Simulation simulation))
