(ns battlebots.sample-bots.bot-four)

;; Look at bot-one example for details

(defn run
  [arena saved-state my-id]
  {:type "MOVE"
   :metadata {:direction (rand-nth [0 1 2 3 4 5 6 7 8 9])}
   :saved-state {}})
