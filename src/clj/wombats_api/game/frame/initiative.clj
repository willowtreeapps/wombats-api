(ns wombats-api.game.frame.initiative)

(defn- get-decision-makers
  "Returns a vector of all the decision maker uuids"
  [arena]
  (reduce (fn [memo row]
            (reduce (fn [decision-makers cell]
                      (if (contains? #{"ai" "player"} (:type cell))
                        (conj decision-makers (:uuid cell))
                        decision-makers))
                    memo row))
          [] arena))

(defn- rotate-initiative
  "rotates `n` players to the front of list"
  ([initiative-order] (rotate-initiative 1 initiative-order))
  ([n initiative-order]
   (concat (take-last n initiative-order)
           (drop-last n initiative-order))))

(defn update-initiative-order
  "Updates the initiative order or sets it if it has not been set"
  [{:keys [initiative-order clean-arena] :as game-state}]
  (assoc game-state :initiative-order (rotate-initiative
                                       (or initiative-order
                                           (get-decision-makers clean-arena)))))
