(ns battlebots.sample-bots.bot-three)

;; Look at bot-one example for details

(defn run
  [arena saved-state my-id]
  {:type "MOVE"
   :metadata {:direction 3}
   :saved-state {}})
