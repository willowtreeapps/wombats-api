(ns battlebots.dao)

(def rounds (atom {:round-one []}))

;; rounds
(defn save-round!
  [round]
  ;; persist the round
  (let [id (:id round)]
    (if id
      (swap! rounds assoc id (assoc round :active true)))))

(defn get-rounds
  ([] (get-rounds true))
  ([active-only]
    (filter (if active-only
              #(:active %)
              #(true))
            @rounds)))

(defn complete-round!
  [id]
  (if (get @rounds id)
    (swap! rounds
           (fn [roundsval]
             (assoc-in roundsval [id :active] false)))))


;; players
(defn add-player!
  [round-id player])
