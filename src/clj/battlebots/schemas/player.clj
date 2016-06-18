(ns battlebots.schemas.player
  (:require [schema.core :as s]))

(s/defschema Bot
  "bot schema"
  {(s/required-key :repo) s/Str
   (s/required-key :name) s/Str})

(s/defschema Player
  "player schema"
  {(s/optional-key :_id) s/Int
   (s/required-key :username) s/Str
   (s/required-key :password) s/Str
   (s/optional-key :roles) [s/Str]
   (s/required-key :bots) [Bot]})

(defn isPlayer
  "Checks if given object is a valid player"
  [player]
  (s/validate Player player))

(defn is-bot?
  [bot]
  (s/validate Bot bot))
