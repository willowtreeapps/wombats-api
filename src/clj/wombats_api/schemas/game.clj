(ns wombats-api.schemas.game
  (:require [schema.core :as s]))

(s/defschema CellMetadata s/Any)

;; TODO Look into schema to see if it supports or statements for schemas
;; ex: PoisonCell || PlayerCell || AICell || BaseCell
(s/defschema Cell {:type s/Str
                   (s/optional-key :transparent) s/Bool
                   (s/optional-key :display) s/Str
                   (s/optional-key :bot-repo) s/Str
                   (s/optional-key :_id) s/Str
                   (s/optional-key :login) s/Str
                   (s/optional-key :hp) s/Int
                   (s/optional-key :uuid) s/Str
                   (s/optional-key :md) CellMetadata})

(s/defschema Row [Cell])

(s/defschema Arena [Row])

(s/defschema MessageChannel s/Any)

(s/defschema Bot {:repo s/Str
                  :name s/Str
                  :contents-url s/Str})

(s/defschema Player {:_id org.bson.types.ObjectId
                     :bots [Bot]
                     :login s/Str
                     :name s/Str
                     :email s/Str
                     :github-id s/Int
                     :avatar_url s/Str
                     (s/optional-key :admin) s/Bool})

;; TODO The way players are added and tracked througout the game needs to be improved.
;; Ensure that the schemas are similar for normal game operation & game simulation.
(s/defschema InGamePlayer {:_id s/Str
                           :login s/Str
                           (s/optional-key :bot-repo) s/Str
                           (s/optional-key :type) s/Str
                           (s/optional-key :uuid) s/Str})

(s/defschema JoinedPlayer {:_id s/Str
                           :login s/Str
                           :bot-repo s/Str})

(s/defschema Frame {:_id org.bson.types.ObjectId
                    :game-id org.bson.types.ObjectId
                    :round-number s/Int
                    :players [InGamePlayer]
                    :map Arena
                    :messages MessageChannel})

(s/defschema SimulationFrame (select-keys Frame [:map :messages :players]))

(s/defschema CommandConfiguration {:MOVE {:tu s/Int}
                         :SHOOT {:tu s/Int}
                         :SET_STATE {:tu s/Int}
                         :SMOOKESCREEN {:tu s/Int}})

(s/defschema PlayerConfiguration {:initial-hp s/Int
                                  :partial-arena-radius s/Int})

(s/defschema AIConfiguration {:initial-hp s/Int
                              :partial-arena-radius s/Int})

(s/defschema GameConfiguration {:game-type s/Str
                                :frames-per-round s/Int
                                :rounds-per-game s/Int
                                :collision-damage-amount s/Int
                                :shot-damage-amount s/Int
                                :smokescreen-duration s/Int
                                :max-players s/Int
                                :initial-time-unit-count s/Int
                                :command-map CommandConfiguration
                                :player PlayerConfiguration
                                :ai AIConfiguration})

(s/defschema Game {:_id org.bson.types.ObjectId
                   :configuration GameConfiguration
                   :initial-arena Arena
                   :players [InGamePlayer]
                   :state s/Str})
