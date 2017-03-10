(ns wombats.game.zakano-code)

(defn get-arena-dimensions
  "returns the dimensions of a given arena (NOTE: NOT 0 indexed)"
  {:added "1.0"
   :defined-in "wombats.arena.utils"}
  [arena]
  (let [x ((comp count first) arena)
        y (count arena)]
    [x y]))

(defn get-in-arena
  "pulls the cell contents out of an arena at given coords"
  {:added "1.0"}
  [[x y] arena]
  (get-in arena [y x]))

(defn modify-orientation
  "Return a new orientation based off a provided orientation and the direction
  you want to turn"
  {:added "1.0"
   :defined "wombats.game.utils"}
  [current-orientation modifier]

  (def ^:private orientations [:n :e :s :w])

  (let [current-idx (.indexOf orientations current-orientation)]
    (if (not= current-idx -1)
      (condp = modifier
        :right (get orientations (mod (inc current-idx) 4))
        :left (get orientations (mod (dec current-idx) 4))
        :about-face (get orientations (mod (+ 2 current-idx) 4))
        current-orientation)
      current-orientation)))

(defn calculate-turn-frontiers
  [{:keys [orientation coords weight cmd-sequence]}]
  (map (fn [next-direction]
         {:orientation (modify-orientation orientation next-direction)
          :coords coords
          :weight (inc weight)
          :cmd-sequence (conj cmd-sequence {:action :turn
                                            :metadata {:direction next-direction}})})
       [:right :left :about-face]))

(defn get-move-coords
  "Gets the updated coords for moving.

  :Note wrapping not assumed."
  {:added "1.0"}
  [[x y] orientation]
  (case orientation
    :n [x (dec y)]
    :e [(inc x) y]
    :s [x (inc y)]
    :w [(dec x) y]))

(defn get-move-frontier-coords
  "Returns the coords from the move command"
  {:added "1.0"}
  ([coords orientation dimensions]
   (get-move-frontier-coords coords orientation dimensions false))
  ([[x y] orientation [dim-x dim-y] wrap?]
   (let [new-coords (get-move-coords [x y] orientation)
         [new-x new-y] new-coords]

     (if wrap?
       (case orientation
         (:n :s) [new-x (mod new-y dim-y)]
         (:e :w) [(mod new-x dim-x) new-y])
       (case orientation
         :n (if (< new-y 0) nil new-coords)
         :w (if (< new-x 0) nil new-coords)
         :e (if (> new-x (dec dim-x)) nil new-coords)
         :s (if (> new-y (dec dim-y)) nil new-coords))))))

(defn calculate-move-frontier
  [{:keys [orientation coords weight cmd-sequence]}
   arena-dimensions
   wrap?]
  (let [coords (get-move-frontier-coords coords orientation arena-dimensions wrap?)]
    (when coords
      {:orientation orientation
       :coords coords
       :weight (inc weight)
       :cmd-sequence (conj cmd-sequence {:action :move})})))

(defn can-safely-occupy-space?
  "Predicate used to determine what cells can pass as frontiers"
  {:added "1.0"}
  [cell]
  (not (contains? #{"wood-barrier" "steel-barrier" "fog"}
                  (get-in cell [:contents :type]))))

(defn filter-frontiers
  "Filters all the possible frontiers, returning only explore-able frontiers"
  {:added "1.0"}
  [frontiers arena explored]
  (filter (fn [{coords :coords}]
            (if (nil? coords) false
                (let [cell (get-in-arena coords arena)
                      uuid (get-in cell [:contents :uuid])]
                  (and (nil? (get explored uuid))
                       (can-safely-occupy-space? cell))))) frontiers))

(defn calculate-frontier
  "Caclulates the new frontier set based off of the provided frontier."
  {:added "1.0"}
  ([frontier arena explored]
   ;; Default to false because this is currently only used for the partial arena
   ;; which has no notion of wrapping.
   (calculate-frontier frontier arena explored false))
  ([frontier arena explored wrap?]
   (filter-frontiers
    (conj (calculate-turn-frontiers frontier)
          (calculate-move-frontier frontier
                                   (get-arena-dimensions arena)
                                   wrap?))
    arena
    explored)))

(defn add-to-sorted-arena
  "Adds a frontier node to the sorted arena"
  {:added "1.0"}
  [sorted-arena
   {{type :type
     uuid :uuid} :contents}
   {weight :weight
    coords :coords
    cmd-sequence :cmd-sequence}]
  (let [formatted-frontier {:weight weight
                            :uuid uuid
                            :coords coords
                            :cmd-sequence cmd-sequence}]
    (update-in sorted-arena
               [weight (keyword type)]
               (fn [coll]
                 (if (not (nil? coll))
                   (conj coll formatted-frontier)
                   [formatted-frontier])))))

(defn to-global-coords
  "Converts local coordinates passed by the partially occluded arena
  to their corresponding global coordinates"
  {:added "1.0"}
  [{[origin-x origin-y] :local-coords
    [global-x global-y] :global-coords
    [dim-x dim-y] :global-dimensions}]
  (fn [[target-x target-y]]
    (let [delta-x (- target-x origin-x)
          delta-y (- target-y origin-y)
          new-x (mod (+ global-x delta-x) dim-x)
          new-y (mod (+ global-y delta-y) dim-y)]
      [new-x new-y])))

(defn get-first-frontier
  [{:keys [local-coords arena]}]
  (let [{{orientation-str :orientation
          uuid :uuid} :contents} (get-in-arena local-coords arena)]
    {:coords local-coords
     :orientation (keyword orientation-str)
     :uuid uuid
     :weight 0
     :cmd-sequence []}))

(defn sort-arena-by-distance-then-type
  "sorts an arena by distance then type"
  {:added "1.0"}
  [{:keys [arena] :as enriched-state}]
  (let [update-global-coords-fn (to-global-coords enriched-state)]
    (loop [frontier [(get-first-frontier enriched-state)]
           explored {}
           sorted-arena []]

      (if (empty? frontier)
        ;; All frontiers have been explored, break out with sorted-arena
        ;; attached to state.
        (assoc enriched-state :sorted-arena sorted-arena)

        (let [frontier-node (first frontier)
              cell (get-in-arena (:coords frontier-node) arena)
              next-frontier (calculate-frontier frontier-node
                                                arena
                                                explored)]
          (recur (vec (concat (rest frontier) next-frontier))
                 (merge explored {(get-in cell [:contents :uuid]) true})
                 (add-to-sorted-arena sorted-arena
                                      cell
                                      (update frontier-node
                                              :coords
                                              update-global-coords-fn))))))))

(defn remove-self
  [uuid]
  (fn [{:keys [wombat] :as weight-map}]
    (if wombat
      (let [filtered-list (vec (filter #(not= uuid (:uuid %)) wombat))]
        (if (empty? filtered-list)
          (dissoc weight-map :wombat)
          (assoc weight-map :wombat filtered-list)))
      weight-map)))

(defn remove-self-from-sorted-arena
  "removes current user from the sorted arena"
  {:added "1.0"}
  [{:keys [local-coords arena my-uuid] :as enriched-state}]
  (update-in
   enriched-state
   [:sorted-arena]
   (fn [sorted-arena]
     (-> sorted-arena
         (update 0 (remove-self my-uuid))
         (update 1 (remove-self my-uuid))))))

(defn track-able-cell?
  [{{type :type} :contents}]
  (not (contains? #{"fog"} type)))

(defn update-in-global-arena
  [global-arena [x y] {{cell-type :type} :contents}]
  (update-in global-arena
            [y x]
            (fn [current-cell]
              (if (nil? current-cell)
                {:type cell-type
                 :explored? false}
                (merge current-cell {:type cell-type})))))

(defn update-global-view
  "updates what your bot has seen historically."
  {:added "1.0"}
  [{:keys [saved-state arena my-uuid global-dimensions] :as enriched-state}]
  (let [[dim-x dim-y] global-dimensions

        global-arena (:global-arena saved-state)

        update-global-coords-fn
        (to-global-coords enriched-state)

        current-global-arena
        (if global-arena
          global-arena
          (vec (repeat dim-y (vec (repeat dim-x nil)))))

        updated-global-arena
        (:global-arena
         (reduce
          (fn [{:keys [y-idx global-arena] :as acc} row]
            {:y-idx (inc y-idx)
             :global-arena
             (:global-arena
              (reduce
               (fn [{:keys [x-idx global-arena]} cell]
                 {:x-idx (inc x-idx)
                  :global-arena (if (track-able-cell? cell)
                                  (update-in-global-arena global-arena
                                                          (update-global-coords-fn [x-idx y-idx])
                                                          cell)
                                  global-arena)})
               {:x-idx 0
                :global-arena global-arena} row))})
          {:y-idx 0
           :global-arena current-global-arena} arena))]

    (assoc enriched-state :global-arena updated-global-arena)))

(defn add-my-uuid
  [{:keys [local-coords arena] :as enriched-state}]
  (let [self (get-in-arena local-coords arena)]
    (assoc enriched-state :my-uuid (get-in self [:contents :uuid]))))

(defn get-first-of
  "Returns the closest item's command sequence that matches the item-type"
  [sorted-arena item-type weight-coll-fn]
  (:cmd-sequence
   (reduce
    (fn [item weight-map]
      (if item
        item
        (when (item-type weight-map)
          (weight-coll-fn (item-type weight-map)))))
    nil
    sorted-arena)))

(defn get-closest-food-seq
  [sorted-arena]
  (get-first-of sorted-arena :food first))

(defn check-if-explored
  [global-arena]
  (fn [open-spaces]
    (let [unexplored (filter (fn [{coords :coords}]
                               (boolean (not (:explored? (get-in-arena coords global-arena)))))
                             open-spaces)]
      (when (not (empty? unexplored))
        (first unexplored)))))

(defn get-furthest-unexplored-seq
  [sorted-arena global-arena]
  (get-first-of (reverse sorted-arena) :open (check-if-explored global-arena)))

(defn choose-command
  [{:keys [sorted-arena global-arena] :as enriched-state}]
  ;; if the zakano doesn't know what to do next, it's
  ;; defense mechanism is to spin and shoot.
  (let [action-sequence (or (get-closest-food-seq sorted-arena)
                            (get-furthest-unexplored-seq sorted-arena global-arena)
                            [{:action :shoot}
                             {:action :turn
                              :metadata {:direction :right}}])
        action (first action-sequence)
        remaining-sequence (vec (rest action-sequence))]
    (merge enriched-state
           {:command action
            :remaining-action-seq remaining-sequence})))

(defn format-response
  "formats the final response object"
  {:added "1.0"}
  [{command :command
    global-arena :global-arena
    global-coords :global-coords
    remaining-action-seq :remaining-action-seq
    {frame-number :frame-number} :saved-state}
   [global-x global-y]]

  {:command command
   :state {:global-arena (assoc-in global-arena [global-y global-x :explored?] true)
           :remaining-action-seq remaining-action-seq
           :frame-number (if frame-number
                           (inc frame-number) 0)}})

(defn enrich-state
  "Adds additional information to the given state used to improve
   the decision-making process"
  {:added "1.0"}
  [{:keys [arena local-coords saved-state] :as state}]
  (-> state
      (add-my-uuid)
      (sort-arena-by-distance-then-type)
      (remove-self-from-sorted-arena)
      (update-global-view)))

(defn process-next-cmd
  "semi-inefficient, this ensures that the zakano blindly follow the first
    action till completion"
  {:added "1.0"}
  [{:keys [remaining-action-seq] :as state}]
  (-> state
      (assoc :command (first remaining-action-seq))
      (assoc :remaining-action-seq (vec (rest remaining-action-seq)))))

(defn has-next-action?
  "Check to see if there is a next action in the action sequence/"
  {:added "1.0"}
  [{:keys [remaining-action-seq]}]
  (boolean (and remaining-action-seq (not (empty? remaining-action-seq)))))

(defn main-fn
  [{:keys [saved-state global-coords] :as state} time-left]

  (if (has-next-action? saved-state)
    (-> saved-state
        (assoc :saved-state saved-state)
        (process-next-cmd)
        (format-response global-coords))

    (-> (enrich-state state)
        (choose-command)
        (format-response global-coords))))


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; End local Bot Testing
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def zakano-fn
  "(fn [state time-left]
     (def turn-directions [:right :left :about-face])
     (def smoke-directions [:forward :backward :left :right :drop])
     (let [command-options [(repeat 10 {:action :move
                                        :metadata {}})
                            (repeat 4 {:action :turn
                                       :metadata {:direction (rand-nth turn-directions)}})
                            (repeat 2 {:action :shoot
                                       :metadata {}})
                            (repeat 1 {:action :smoke
                                       :metadata {:direction (rand-nth smoke-directions)}})]]
      {:command (rand-nth (flatten command-options))
       :state {}}))")

(defn get-zakano-code [] zakano-fn)
