(ns wombats.game.zakano-code)

(def zakano-fn
  "(fn [state time-left]
    (def turn-directions [:right :left :about-face])

    (let [command-options [{:command {:action :move
                                      :metadata {}}}
                           {:command {:action :turn
                                      :metadata (rand-nth turn-directions)}}
                           {:command {:action :shoot
                                      :metadata {}}}]]
      {:command (rand-nth command-options)
       :state {:test true}}))")

(defn get-zakano-code
  []
  zakano-fn)
