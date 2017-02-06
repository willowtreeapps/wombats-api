(ns wombats.game.zakano-code)

(def zakano-fn
  "(fn [state time-left]
     (def turn-directions [:right :left :about-face])

     (let [command-options [(repeat 10 {:command {:action :move
                                                  :metadata {}}})
                            (repeat 2 {:command {:action :turn
                                                 :metadata {:direction (rand-nth turn-directions)}}})
                            (repeat 4 {:command {:action :shoot
                                                 :metadata {}}})]]
       {:command (rand-nth (flatten command-options))
        :state {:test true}}))")

(defn get-zakano-code
  []
  zakano-fn)
