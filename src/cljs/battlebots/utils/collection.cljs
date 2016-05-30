(ns battlebots.utils.collection)

(defn update-or-insert
  "updates a record in a collection, or inserts it if it does not exist"
  [collection update & {:keys [matcher] :or {matcher :_id}}]
  (let [matcher (or matcher :_id)
        operation (reduce (fn [memo record]
                            (let [should-update? (= (matcher record) (matcher update))]
                              (if should-update?
                                (merge memo {:collection (conj (:collection memo) update)
                                             :updated true})
                                (merge memo {:collection (conj (:collection memo) record)}))))
                          {:collection []
                           :updated false}
                          collection)]
    (if (:updated operation)
      (:collection operation)
      (conj (:collection operation) update))))
