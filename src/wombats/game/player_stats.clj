(ns wombats.game.player-stats)

(defn- add-player-scores
  [stats players]
  (reduce
   (fn [stats-acc [uuid {:keys [stats user wombat player]}]]
     (let [score (:stats/score stats)
           user (:user/github-username user)
           wombat (:wombat/name wombat)
           color (:player/color player)]
       (assoc stats-acc uuid {:score score
                              :username user
                              :wombat-name wombat
                              :color color})))
   stats players))

(defn- add-player-hp
  [stats arena]
  (let [player-ids (set (keys stats))]
    (reduce
     (fn [stats-acc cell]
       (let [cell-uuid (get-in cell [:contents :uuid])]
         (if (contains? player-ids cell-uuid)
           (assoc-in stats-acc [cell-uuid :hp] (get-in cell [:contents :hp]))
           stats-acc)))
     stats (flatten arena))))

(defn get-player-stats
  "Returns stats from game-state"
  [game-state]
  (-> {}
      (add-player-scores (:players game-state))
      (add-player-hp (get-in game-state [:frame
                                         :frame/arena]))
      vals))
