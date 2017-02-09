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

(defn- update-deterioration
  "Determines the deterioration level and applies it to the element"
  [{:keys [contents] :as cell} max-hp]
  (let [percent-deteriorated (-> (:hp contents)
                                 (/ max-hp)
                                 (float))
        deterioration-key (condp >= percent-deteriorated
                            0.4 :high
                            0.7 :medium
                            :low)]
    (assoc-in cell [:contents :deterioration-level] deterioration-key)))

(defn- update-cell-deterioration-level
  [cell arena-config]
  (case (get-in cell [:contents :type])
    :wombat (update-deterioration cell (:arena/wombat-hp arena-config))
    :zakano (update-deterioration cell (:arena/zakano-hp arena-config))
    :wood-barrier (update-deterioration cell (:arena/wood-wall-hp arena-config))
    :steel-barrier (update-deterioration cell (:arena/steel-wall-hp arena-config))
    cell))

(defn- update-arena-data
  "Maps over each cell updating attributes like metadata decay and damage levels"
  [{:keys [arena-config] :as game-state}]
  (let [updated-arena
        (vec (map (fn [row]
                    (vec (map #(-> %
                                   (update-cell-metadata)
                                   (update-cell-deterioration-level arena-config)) row)))
                  (get-in game-state [:frame :frame/arena])))]
    (assoc-in game-state [:frame :frame/arena] updated-arena)))

(defn finalize-frame
  [game-state]
  ;; TODO #152
  (-> game-state
      (update-arena-data)))

(defn finalize-game
  [game-state]
  ;; TODO #152
  game-state)
