(ns wombats.game.finalizers)

(defn- update-cell-metadata
  [{:keys [meta] :as cell}]
  (let [meta-update (reduce (fn [meta-acc meta-item]
                              (if (or (not (boolean (:decay meta-item)))
                                      (= (:decay meta-item) 0))
                                meta-acc
                                (conj meta-acc
                                      (update meta-item :decay dec))))
                            [] meta)]
    (assoc cell :meta meta-update)))

(defn- update-metadata-decay
  [game-state]
  (let [updated-arena
        (vec (map (fn [row]
                    (vec (map update-cell-metadata row)))
                  (get-in game-state [:frame :frame/arena])))]
    (assoc-in game-state [:frame :frame/arena] updated-arena)))

(defn finalize-frame
  [game-state]
  ;; TODO #152
  (-> game-state
      (update-metadata-decay)))

(defn finalize-game
  [game-state]
  ;; TODO #152
  game-state)
