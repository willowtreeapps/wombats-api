(ns wombats.game.zakano-code
  (:refer-clojure :exclude [fn])
  (:require [serializable.fn :refer :all]))

(defmacro defsource
  "Similar to clojure.core/defn, but saves the function's definition in the var's
   :source meta-data.

  http://stackoverflow.com/questions/3782970/how-can-i-display-the-definition-of-a-function-in-clojure-at-the-repl"
  {:arglists (:arglists (meta (var defn)))}
  [fn-name & defn-stuff]
  `(do (defn ~fn-name ~@defn-stuff)
       (alter-meta! (var ~fn-name) assoc :source (quote ~&form))
       (var ~fn-name)))

(defsource get-arena-dimensions
  "returns the dimensions of a given arena (NOTE: NOT 0 indexed)"
  {:added "1.0"
   :defined-in "wombats.arena.utils"}
  [arena]
  (let [x ((comp count first) arena)
        y (count arena)]
    [x y]))

(defsource get-in-arena
  "pulls the cell contents out of an arena at given coords"
  {:added "1.0"}
  [[x y] arena]
  (get-in arena [y x]))

(defsource modify-orientation
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

(defsource calculate-turn-frontiers
  [{:keys [orientation coords weight action-sequence]}]
  (map (fn [next-direction]
         {:orientation (modify-orientation orientation next-direction)
          :coords coords
          :weight (inc weight)
          :action-sequence (conj action-sequence {:action :turn
                                                  :metadata {:direction next-direction}})})
       [:right :left :about-face]))

(defsource get-move-coords
  "Gets the updated coords for moving.

  :Note wrapping not assumed."
  {:added "1.0"}
  [[x y] orientation]
  (case (keyword orientation)
    :n [x (dec y)]
    :e [(inc x) y]
    :s [x (inc y)]
    :w [(dec x) y]))

(defsource get-move-frontier-coords
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

(defsource calculate-move-frontier
  [{:keys [orientation coords weight action-sequence]}
   arena-dimensions
   wrap?]
  (let [coords (get-move-frontier-coords coords orientation arena-dimensions wrap?)]
    (when coords
      {:orientation orientation
       :coords coords
       :weight (inc weight)
       :action-sequence (conj action-sequence {:action :move})})))

(defsource can-safely-occupy-space?
  "Predicate used to determine what cells can pass as frontiers"
  {:added "1.0"}
  [cell]
  (not (contains? #{"wood-barrier" "steel-barrier" "fog"}
                  (get-in cell [:contents :type]))))

(defsource filter-frontiers
  "Filters all the possible frontiers, returning only explore-able frontiers"
  {:added "1.0"}
  [frontiers arena explored]
  (filter (fn [{coords :coords}]
            (if (nil? coords) false
                (let [cell (get-in-arena coords arena)
                      uuid (get-in cell [:contents :uuid])]
                  (and (nil? (get explored uuid))
                       (can-safely-occupy-space? cell))))) frontiers))

(defsource calculate-frontier
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

(defsource add-to-sorted-arena
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

(defsource to-global-coords
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

(defsource get-first-frontier
  [{:keys [local-coords arena]}]
  (let [{{orientation-str :orientation
          uuid :uuid} :contents} (get-in-arena local-coords arena)]
    {:coords local-coords
     :orientation (keyword orientation-str)
     :uuid uuid
     :weight 0
     :action-sequence []}))

(defsource sort-arena-by-distance-then-type
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

(defsource remove-self
  [uuid]
  (fn [{:keys [wombat] :as weight-map}]
    (if wombat
      (let [filtered-list (vec (filter (fn [wombat]
                                         (not= uuid (:uuid wombat)))
                                       wombat))]
        (if (empty? filtered-list)
          (dissoc weight-map :wombat)
          (assoc weight-map :wombat filtered-list)))
      weight-map)))

(defsource remove-self-from-sorted-arena
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

(defsource update-in-global-arena
  [global-arena [x y] {{cell-type :type} :contents}]
  (update-in global-arena
             [y x]
             (fn [current-cell]
               (if (nil? current-cell)
                 {:type (name cell-type)
                  :explored? false}
                 (merge current-cell {:type (name cell-type)})))))

(defsource track-able-cell?
  [{{type :type} :contents}]
  (not (contains? #{"fog"} type)))

(defsource add-to-global-arena
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

(defsource get-current-global-arena
  [global-arena [dim-x dim-y]]
  (if global-arena
    global-arena
    (vec (repeat dim-y (vec (repeat dim-x nil))))))

(defsource add-explored-to-global-arena
  [global-arena [global-x global-y]]
  (assoc-in global-arena [global-y global-x :explored?] true))

(defsource update-global-view
  "updates what your bot has seen historically."
  {:added "1.0"}
  [{:keys [saved-state arena global-dimensions global-coords] :as enriched-state}]
  (assoc enriched-state :global-arena
         (-> (get-current-global-arena (:global-arena saved-state)
                                       global-dimensions)
             (add-to-global-arena arena (to-global-coords enriched-state))
             (add-explored-to-global-arena global-coords))))

(defsource add-self
  [{:keys [local-coords arena] :as enriched-state}]
  (let [self (get-in-arena local-coords arena)]
    (assoc enriched-state :self self)))

(defsource update-frame-number
  [{:keys [saved-state] :as enriched-state}]
  (let [frame-number (:frame-number saved-state)]
    (assoc enriched-state
           :frame-number
           (if frame-number (inc frame-number) 0))))

(defsource get-look-ahead-coords
  "Returns the coordinates for all look-ahead cells"
  [{:keys [global-coords global-dimensions self]}
   look-ahead-distance]
  (let [orientation (get-in self [:contents :orientation])]
    (loop [coords []
           current-coords global-coords]
      (if (= (count coords) look-ahead-distance)
        coords
        (let [next-coords
              (get-move-frontier-coords current-coords
                                        orientation
                                        global-dimensions
                                        true)]

          (recur (conj coords next-coords)
                 next-coords))))))

(defsource get-look-ahead-items
  "Get n numnber of cells in front of slef"
  [{:keys [global-arena] :as enriched-state}
   look-ahead-distance]
  (vec
   (map (fn [coords]
          (:type (get-in-arena coords global-arena)))
        (get-look-ahead-coords enriched-state look-ahead-distance))))

(defsource get-first-of
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

(defsource closest-food-action
  [{:keys [sorted-arena]}]
  (get-first-of sorted-arena :food first))

(defsource closest-food-validation
  [{{[x y] :coords} :metadata} global-arena]
  (= "food" (get-in global-arena [y x :type])))

(defsource food-equality
  [prev-command next-command]
  ;; TODO check to see it the sequence in next-command is more efficient
  next-command)

(defsource pathfinding-action
  [enriched-state]
  (let [look-ahead-items (set (get-look-ahead-items enriched-state 3))
        should-shoot (some (fn [element]
                             (contains? look-ahead-items element))
                           ["wood-barrier"
                            "wombat"
                            "zakano"])
        should-turn (some (fn [element]
                            (contains? look-ahead-items element))
                          ["poison"
                           "steel-barrier"])]
    {:action-sequence [(cond
                         should-shoot {:action :shoot}
                         should-turn {:action :turn
                                      :metadata {:direction :right}}
                         :else {:action :move})]}))

(defsource fire-action
  [enriched-state]
  (let [look-ahead-items (set (get-look-ahead-items enriched-state 3))
        should-shoot (some (fn [element]
                             (contains? look-ahead-items element))
                           ["zakano"
                            "wombat"])]))

(defsource clueless-action
  ;; if the zakano doesn't know what to do next, it's
  ;; defense mechanism is to spin and shoot.
  [_]
  {:action-sequence [{:action :turn
                      :metadata {:direction :right}}
                     {:action :shoot}]})

(defsource format-command
  ([action-name action-sequence]
   (format-command action-name action-sequence {}))
  ([action-name action-sequence metadata]
   {:action-name action-name
    :command (first action-sequence)
    :remaining-action-seq (vec (rest action-sequence))
    :metadata metadata}))

(defsource xform-command
  [enriched-state action-name action-fn]
  (let [{action-sequence :action-sequence
         metadata :metadata} (action-fn enriched-state)]
    (when action-sequence (format-command action-name action-sequence metadata))))

(defsource format-prev-command
  [{:keys [action-name remaining-action-seq metadata] :as prev-command}]
  (when (and prev-command (not (empty? remaining-action-seq)))
    (format-command action-name remaining-action-seq metadata)))

(defsource get-command-priority
  []
  [{:name "fire!"
    :fn fire-action
    :validate-command (fn [] true)
    :equality-command (fn [prev next] next)}
   {:name "food"
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

(defsource calculate-next-command
  [enriched-state]
  (first (filter (fn [command] (not (nil? command)))
                 (map (fn [command]
                        (xform-command enriched-state
                                       (:name command)
                                       (:fn command)))
                      (get-command-priority)))))

(defsource calculate-optimal-command
  [prev-command next-command global-arena]
  (let [command-priority (get-command-priority)
        commands (map (fn [{:keys [name]}] name) command-priority)
        prev-weight (.indexOf commands (:action-name prev-command))
        next-weight (.indexOf commands (:action-name next-command))
        prev-command-check (get-in command-priority [prev-weight :validate-command])
        equality-command-check (get-in command-priority [prev-weight :equality-command])]

    (cond
      (= next-weight prev-weight) (equality-command-check prev-command next-command)
      (and (> prev-weight next-weight)
           (prev-command-check prev-command global-arena)) prev-command
      :else next-command)))

(defsource choose-command
  [{:keys [saved-state sorted-arena global-arena] :as enriched-state}]
  (let [prev-command (format-prev-command (:prev-command saved-state))
        next-command (calculate-next-command enriched-state)
        selected-command (if prev-command
                           (calculate-optimal-command prev-command
                                                      next-command
                                                      global-arena)
                           next-command)]
    (assoc enriched-state :next-command selected-command)))

(defsource format-response
  "formats the final response object"
  {:added "1.0"}
  [{global-arena :global-arena
    next-command :next-command
    frame-number :frame-number}
   time-left]

  {:command (:command next-command)
   :state {:global-arena global-arena
           :prev-command next-command
           :frame-number frame-number
           :time-left (str (time-left))}})

(defsource enrich-state
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

(defsource main-fn
  [state time-left]
  (-> (enrich-state state)
      (choose-command)
      (format-response time-left)))

;; Format Zakano Response
(def zakano-fns
  [#'get-arena-dimensions
   #'get-in-arena
   #'modify-orientation
   #'calculate-turn-frontiers
   #'get-move-coords
   #'get-move-frontier-coords
   #'calculate-move-frontier
   #'can-safely-occupy-space?
   #'filter-frontiers
   #'calculate-frontier
   #'add-to-sorted-arena
   #'to-global-coords
   #'get-first-frontier
   #'sort-arena-by-distance-then-type
   #'remove-self
   #'remove-self-from-sorted-arena
   #'update-in-global-arena
   #'track-able-cell?
   #'add-to-global-arena
   #'get-current-global-arena
   #'add-explored-to-global-arena
   #'update-global-view
   #'add-self
   #'update-frame-number
   #'get-look-ahead-coords
   #'get-look-ahead-items
   #'get-first-of
   #'closest-food-action
   #'closest-food-validation
   #'food-equality
   #'pathfinding-action
   #'fire-action
   #'clueless-action
   #'format-command
   #'xform-command
   #'format-prev-command
   #'get-command-priority
   #'calculate-next-command
   #'calculate-optimal-command
   #'choose-command
   #'format-response
   #'enrich-state
   #'main-fn])

(defn get-definition [v]
  (clojure.string/replace (:source (meta v))
                          #"defsource"
                          "defn"))

(def zakano-fn
  (format "(fn [state time-left] %s (main-fn state time-left))"
          (clojure.string/join ""
                               (map get-definition zakano-fns))))

(defn get-zakano-code [] zakano-fn)
