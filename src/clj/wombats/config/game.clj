(ns wombats.config.game)

(def ^:private initial-player {:initial-hp 10000
                               :partial-arena-radius 10})

(def ^:private initial-ai {:partial-arena-radius 2
                           :initial-hp 10})

(def config
  {:frames-per-round 30
   :rounds-per-game 2
   :collision-damage-amount 10
   :shot-damage-amount 10
   :command-map {:MOVE {:tu 50}
                 :SHOOT {:tu 100}
                 :SET_STATE {:tu 0}}
   :initial-time-unit-count 100
   :player initial-player
   :ai initial-ai})
