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
  [stats arena wombat-hp]
  (let [player-ids (set (keys stats))]
    (reduce
     (fn [stats-acc cell]
       (let [cell-uuid (get-in cell [:contents :uuid])]
         (if (contains? player-ids cell-uuid)
           (let [hp (get-in cell [:contents :hp])
                 percent (* (double (/ hp wombat-hp)) 100)]
             (assoc-in stats-acc [cell-uuid :hp] percent))
           stats-acc)))
     stats (flatten arena))))

(defn get-player-stats
  "Returns stats from game-state"
  [{:keys [players frame arena-config]}]
  (-> {}
      (add-player-scores players)
      (add-player-hp (:frame/arena frame)
                     (:arena/wombat-hp arena-config))
      vals))
