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
  [{:keys [orientation coords weight action-sequence]}]
  (map (fn [next-direction]
         {:orientation (modify-orientation orientation next-direction)
          :coords coords
          :weight (inc weight)
          :action-sequence (conj action-sequence {:action :turn
                                                  :metadata {:direction next-direction}})})
       [:right :left :about-face]))

(defn get-move-coords
  "Gets the updated coords for moving.

  :Note wrapping not assumed."
  {:added "1.0"}
  [[x y] orientation]
  (case (keyword orientation)
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
       (case (keyword orientation)
         (:n :s) [new-x (mod new-y dim-y)]
         (:e :w) [(mod new-x dim-x) new-y])
       (case (keyword orientation)
         :n (if (< new-y 0) nil new-coords)
         :w (if (< new-x 0) nil new-coords)
         :e (if (> new-x (dec dim-x)) nil new-coords)
         :s (if (> new-y (dec dim-y)) nil new-coords))))))

(defn calculate-move-frontier
  [{:keys [orientation coords weight action-sequence]}
   arena-dimensions
   wrap?]
  (let [coords (get-move-frontier-coords coords orientation arena-dimensions wrap?)]
    (when coords
      {:orientation orientation
       :coords coords
       :weight (inc weight)
       :action-sequence (conj action-sequence {:action :move})})))

(defn can-safely-occupy-space?
  "Predicate used to determine what cells can pass as frontiers"
  {:added "1.0"}
  [cell]
  (not (contains? #{:wood-barrier :steel-barrier :fog}
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
    action-sequence :action-sequence}]
  (let [formatted-frontier {:weight weight
                            :uuid uuid
                            :coords coords
                            :action-sequence action-sequence}]
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
     :action-sequence []}))

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
  [{:keys [local-coords arena self] :as enriched-state}]
  (update-in
   enriched-state
   [:sorted-arena]
   (fn [sorted-arena]
     (-> sorted-arena
         (update 0 (remove-self (:uuid self)))
         (update 1 (remove-self (:uuid self)))))))

(defn update-in-global-arena
  [global-arena [x y] {{cell-type :type} :contents}]
  (update-in global-arena
            [y x]
            (fn [current-cell]
              (if (nil? current-cell)
                {:type (name cell-type)
                 :explored? false}
                (merge current-cell {:type (name cell-type)})))))

(defn track-able-cell?
  [{{type :type} :contents}]
  (not (contains? #{"fog"} type)))

(defn add-to-global-arena
  [global-arena partial-arena update-global-coords-fn]
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
     :global-arena global-arena} partial-arena)))

(defn get-current-global-arena
  [global-arena [dim-x dim-y]]
  (if global-arena
    global-arena
    (vec (repeat dim-y (vec (repeat dim-x nil))))))

(defn add-explored-to-global-arena
  [global-arena [global-x global-y]]
  (assoc-in global-arena [global-y global-x :explored?] true))

(defn update-global-view
  "updates what your bot has seen historically."
  {:added "1.0"}
  [{:keys [saved-state arena global-dimensions global-coords] :as enriched-state}]
  (assoc enriched-state :global-arena
         (-> (get-current-global-arena (:global-arena saved-state)
                                       global-dimensions)
             (add-to-global-arena arena (to-global-coords enriched-state))
             (add-explored-to-global-arena global-coords))))

(defn add-self
  [{:keys [local-coords arena] :as enriched-state}]
  (let [self (get-in-arena local-coords arena)]
    (assoc enriched-state :self self)))

(defn update-frame-number
  [{:keys [saved-state] :as enriched-state}]
  (let [frame-number (:frame-number saved-state)]
    (assoc enriched-state
           :frame-number
           (if frame-number (inc frame-number) 0))))

(defn get-first-of
  "Returns the closest item's command sequence that matches the item-type"
  [sorted-arena item-type weight-coll-fn]
  (let [{action-sequence :action-sequence
         coords :coords}
        (reduce
         (fn [item weight-map]
           (if item
             item
             (when (item-type weight-map)
               (weight-coll-fn (item-type weight-map)))))
         nil
         sorted-arena)]
    (when action-sequence
      {:action-sequence action-sequence
       :metadata {:coords coords}})))

(defn closest-food-action
  [{:keys [sorted-arena]}]
  (get-first-of sorted-arena :food first))

(defn closest-food-validation
  [{{[x y] :coords} :metadata} global-arena]
  (= "food" (get-in global-arena [y x :type])))

(defn food-equality
  [prev-command next-command]
  ;; TODO check to see it the sequence in next-command is more efficient
  next-command)

(defn pathfinding-action
  [{:keys [global-arena global-coords self global-dimensions]}]
  (let [orientation (get-in self [:contents :orientation])
        look-ahead 3 ;; TODO This should be passed in based off a l.o.s.
        look-ahead-coords (loop [coords []
                                 current-coords global-coords]
                            (if (= (count coords) look-ahead)
                              coords
                              (let [next-coords (get-move-frontier-coords current-coords
                                                                          orientation
                                                                          global-dimensions
                                                                          true)]

                                (recur (conj coords next-coords)
                                       next-coords))))
        look-ahead-items (set (map #(:type (get-in-arena % global-arena)) look-ahead-coords))
        should-shoot? (some #(contains? look-ahead-items %) ["steel-barrier"
                                                             "wood-barrier"
                                                             "wombat"
                                                             "zakano"])
        should-turn? (contains? look-ahead-items "poison")]

    {:action-sequence [(cond
                         should-shoot? {:action :shoot}
                         should-turn? {:action :turn
                                       :metadata {:direction :right}}
                         :else {:action :move})]}))

(defn clueless-action
  ;; if the zakano doesn't know what to do next, it's
  ;; defense mechanism is to spin and shoot.
  [_]
  {:action-sequence [{:action :turn
                      :metadata {:direction :right}}
                     {:action :shoot}]})

(defn format-command
  ([action-name action-sequence]
   (format-command action-name action-sequence {}))
  ([action-name action-sequence metadata]
   {:action-name action-name
    :command (first action-sequence)
    :remaining-action-seq (vec (rest action-sequence))
    :metadata metadata}))

(defn xform-command
  [enriched-state action-name action-fn]
  (let [{action-sequence :action-sequence
         metadata :metadata} (action-fn enriched-state)]
    (when action-sequence (format-command action-name action-sequence metadata))))

(defn format-prev-command
  [{:keys [action-name remaining-action-seq metadata] :as prev-command}]
  (when (and prev-command (not (empty? remaining-action-seq)))
    (format-command action-name remaining-action-seq metadata)))

(def command-priority
  [{:name "food"
    :fn closest-food-action
    :validate-command closest-food-validation
    :equality-command food-equality}
   {:name "pathfinding"
    :fn pathfinding-action
    :validate-command (fn [] true)
    :equality-command (fn [prev next] next)}
   {:name "clueless"
    :fn clueless-action
    :validate-command (fn [] true)
    :equality-command (fn [prev next] prev)}])

(defn calculate-next-command
  [enriched-state]
  (first (filter #(not (nil? %))
                 (map #(xform-command enriched-state (:name %) (:fn %))
                      command-priority))))

(defn calculate-optimal-command
  [prev-command next-command global-arena]
  (let [commands (map #(:name %) command-priority)
        prev-weight (.indexOf commands (:action-name prev-command))
        next-weight (.indexOf commands (:action-name next-command))
        prev-command-check (get-in command-priority [prev-weight :validate-command])
        equality-command-check (get-in command-priority [prev-weight :equality-command])]

    (cond
      (= next-weight prev-weight) (equality-command-check prev-command next-command)
      (and (> prev-weight next-weight)
           (prev-command-check prev-command global-arena)) prev-command
      :else next-command)))

(defn choose-command
  [{:keys [saved-state sorted-arena global-arena] :as enriched-state}]
  (let [prev-command (format-prev-command (:prev-command saved-state))
        next-command (calculate-next-command enriched-state)
        selected-command (if prev-command
                           (calculate-optimal-command prev-command
                                                      next-command
                                                      global-arena)
                           next-command)]
    (assoc enriched-state :next-command selected-command)))

(defn format-response
  "formats the final response object"
  {:added "1.0"}
  [{global-arena :global-arena
    next-command :next-command
    frame-number :frame-number}]

  {:command (:command next-command)
   :state {:global-arena global-arena
           :prev-command next-command
           :frame-number frame-number}})

(defn enrich-state
  "Adds additional information to the given state used to improve
   the decision-making process"
  {:added "1.0"}
  [state]
  (-> state
      (add-self)
      (sort-arena-by-distance-then-type)
      (remove-self-from-sorted-arena)
      (update-global-view)
      (update-frame-number)))

(defn main-fn
  [state time-left]
  (-> (enrich-state state)
      (choose-command)
      (format-response)))

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
