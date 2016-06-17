(ns battlebots.game
  (:require [battlebots.arena :as arena]
            [battlebots.services.mongodb :as db]
            [battlebots.constants.arena :refer [arena-key]]
            [battlebots.constants.game :refer [segment-length game-length]]
            [battlebots.sample-bots.bot-one :as bot-one]     ;; TODO Remove sample bots when users can register
            [battlebots.sample-bots.bot-two :as bot-two]
            [battlebots.sample-bots.bot-three :as bot-three]
            [battlebots.sample-bots.bot-four :as bot-four]))

;;
;; HELPER FUNCTIONS
;;

;; Modified from
;; http://stackoverflow.com/questions/4830900/how-do-i-find-the-index-of-an-item-in-a-vector
(defn position
  [pred coll]
  (first (keep-indexed (fn [idx x]
                         (when (pred x)
                           idx))
                       coll)))

(defn total-rounds
  "Calculates the total number of rounds that have elapsed"
  [rounds segments]
  (+ (* segments segment-length) rounds))

(defn get-arena-row-cell-length
  "Similar to arena/get-arena-dimensions except that it is zero based"
  [arena]
  (let [x (count arena)
        y ((comp count first) arena)]
    [(dec x) (dec y)]))

(defn get-player
  "Grabs a player by id"
  [id collection]
  (first (filter #(= (:_id %) id) collection)))

(defn sanitized-player
  "Sanitizes the full player object returning the partial used on the game map"
  [player]
  (select-keys player [:_id :login]))

(defn save-segment
  [{:keys [_id players rounds segment-count] :as game-state}]
  (db/save-game-segment {:game-id _id
                         :players (map sanitized-player players)
                         :rounds rounds
                         :segment segment-count}))

(defn get-item
  "gets an item based off of given coords"
  [coords arena]
  (get-in arena coords))

(defn randomize-players
  "Randomizes player ids"
  [players]
  (shuffle (map #(:_id %) players)))

(defn sanitize-player
  [player]
  (dissoc player :bot :saved-state))

(defn sort-decisions
  "Sorts player decisions based of of a provided execution-order"
  [decisions execution-order]
  (map #(get-player % decisions) execution-order))

(defn get-player-coords
  "Returns a tuple of a given players coords

  TODO: There's most likely a better way to accomplish this"
  [_id arena]
  (:coords (reduce (fn [memo row]
                     (if (:coords memo)
                       memo
                       (let [idx (position #(= (:_id %) _id) row)
                             row-number (:row memo)]
                         (if idx
                           {:row (+ 1 row-number)
                            :coords [row-number idx]}
                           {:row (+ 1 row-number)}))))
                   {:row 0} arena)))

(defn wrap-coords
  "wraps the coords around to the other side of the arena"
  [[c-x c-y] [d-x d-y]]
  (let [x (cond
           (< c-x 0) d-x
           (> c-x d-x) 0
           :else c-x)
        y (cond
           (< c-y 0) d-y
           (> c-y d-y) 0
           :else c-y)]
    [x y]))

(defn adjust-coords
  "Returns a new set of coords based off of an applied direction.

  0 1 2
  7   3
  6 5 4

  "
  [coords direction dimensions]
  (let [
        updater (cond
                 (= direction 0) [dec dec]
                 (= direction 1) [dec identity]
                 (= direction 2) [dec inc]
                 (= direction 3) [identity inc]
                 (= direction 4) [inc inc]
                 (= direction 5) [inc identity]
                 (= direction 6) [inc dec]
                 (= direction 7) [identity dec]
                 :else [identity identity])
        coords (map #(%1 %2) updater coords)]
    (wrap-coords coords dimensions)))

(defn can-occupy-space?
  "determins if a bot can occupy a given space"
  [{:keys [type] :as space}]
  (or
   (= type "open")
   (= type "food")
   (= type "poison")))

(defn determin-affects
  "Determins how player stats should be affected"
  [{:keys [type] :as space}]
  (cond
   (= type "open") {}
   (= type "food") {:score #(+ % 10)}
   (= type "poison") {:score #(- % 5)}))

(defn apply-player-update
  "Applies an update to a player object"
  [player update]
  (reduce (fn [player [prop update-fn]]
            (assoc player prop (update-fn (get player prop)))) player update))

(defn modify-player-stats
  [player-id update players]
  (map (fn [{:keys [_id] :as player}]
         (if (= player-id _id)
           (apply-player-update player update)
           player)) players))

(defn player-occupy-space
  [coords player-id]
  (fn [{:keys [dirty-arena players] :as game-state}]
     (let [cell-contents (get-item coords dirty-arena)
           player (get-player player-id players)
           updated-arena (arena/update-cell dirty-arena coords (sanitized-player player))
           player-update (determin-affects cell-contents)
           updated-players (modify-player-stats player-id player-update players)]
       (merge game-state {:dirty-arena updated-arena
                          :players updated-players}))))

(defn clear-space
  [coords]
  (fn [{:keys [dirty-arena] :as game-state}]
     (let [updated-arena (arena/update-cell dirty-arena coords (:open arena-key))]
       (merge game-state {:dirty-arena updated-arena}))))

;;
;; DECISION FUNCTIONS
;;
;; Each decision function takes the _id (of the user / bot making the decision,
;; the decision map, and the game-state. Once the decision is applied, the decision
;; function will return a modifed game-state (:dirty-arena)

(defn move-player
  [_id {:keys [direction] :as metadata} {:keys [dirty-arena players] :as game-state}]
  (let [player-coords (get-player-coords _id dirty-arena)
        dimensions (get-arena-row-cell-length dirty-arena)
        desired-coords (adjust-coords player-coords direction dimensions)
        desired-space-contents (get-item desired-coords dirty-arena)
        take-space? (can-occupy-space? desired-space-contents)]
    (if take-space?
      (reduce #(%2 %1) game-state [(clear-space player-coords)
                                   (player-occupy-space desired-coords _id)])
      (reduce #(%2 %1) game-state []))))

;;
;; GAME STATE UPDATE HELPERS
;;

(defn apply-decision
  "Applies player decision to the current state of the game"
  [game-state {:keys [decision _id]}]
  (let [type (:type decision)
        metadata (:metadata decision)]
    (cond
     (= type "MOVE") (move-player _id metadata game-state)
     (= type "SHOOT" game-state) ;; TODO
     (= type "HEAL" game-state) ;; TODO
     :else game-state)))

(defn resolve-player-decisions
  "Returns a vecor of player decisions based off of the logic provided by
  their bots and an identical clean version of the arena"
  [players clean-arena]
  (map (fn [{:keys [_id bot saved-state] :as player}] {:decision (bot clean-arena saved-state _id)
                                                       :_id _id}) players))

;;
;; GAME STATE UPDATERS
;;
;; Each one of these functions takes a game-state map and returns
;; a game-state map
;;

(defn resolve-player-turns
  "Updates the arena by applying each players movement logic"
  [{:keys [players clean-arena] :as game-state}]
  (let [execution-order (randomize-players players)
        player-decisions (resolve-player-decisions players clean-arena)
        sorted-player-decisions (sort-decisions player-decisions execution-order)
        updated-game-state (reduce apply-decision game-state sorted-player-decisions)]
    updated-game-state))

;;
;; GAME INITIALIZERS / FINALIZERS
;;
;; Game initializers are intended to provide additional information to
;; not stored in the game object
;;

(defn initialize-players
  "Preps each player map for the game. This player map is different from
  the one that is contained inside of the arena and will contain private data
  including scores, decision logic, and saved state."
  [players]

  ;; TODO implement user specified bots
  (let [bots [bot-one/run bot-two/run bot-three/run bot-four/run]]
    (map #(merge % {:score 0
                    :bot (rand-nth bots)
                    :saved-state {}}) players)))

(defn initialize-game
  "Preps the game"
  [{:keys [initial-arena players] :as game}]
  (merge game {:clean-arena initial-arena
               :rounds []
               :round-count 0
               :segment-count 0
               :players (initialize-players players)}))

(defn initialize-new-round
  "Preps game-state for a new round"
  [{:keys [clean-arena] :as game-state}]
  (merge game-state {:dirty-arena clean-arena}))

(defn finalize-segment
  "Batches a segment of rounds together, persists them, and returns a clean segment"
  [{:keys [segment-count] :as game-state}]
  (save-segment game-state)
  (merge game-state {:round-count 0
                     :segment-count (inc segment-count)
                     :rounds []}))

(defn finalize-round
  "Modifies game state to close out a round"
  [{:keys [rounds dirty-arena players] :as game-state}]
  (let [formatted-round {:map dirty-arena
                         :players (map sanitize-player players)}
        updated-game-state (merge game-state {:rounds (conj rounds formatted-round)
                                              :clean-arena dirty-arena})]
    (if (= (count (:rounds updated-game-state)) segment-length)
      (finalize-segment updated-game-state)
      updated-game-state)))

(defn finalize-game
  "Finializes game"
  [{:keys [players] :as game-state}]
  (save-segment game-state)
  (merge (dissoc game-state
                 :clean-arena
                 :dirty-arena
                 :rounds
                 :round-count
                 :segment-count) {:state "finalized"
                                  :players (map sanitize-player players)}))

;;
;; MAIN GAME LOOP
;;

(defn start-game
  "Starts the game loop"
  [{:keys [players initial-arena] :as game}]
  (let [initial-game-state (initialize-game game)
        final-game-state (loop [{:keys [round-count segment-count] :as game-state} initial-game-state]
                           (if (< (total-rounds round-count segment-count) game-length)
                             (let [updated-game-state (reduce (fn [game-state update-function]
                                                                (update-function game-state))
                                                              (initialize-new-round game-state)
                                                              [resolve-player-turns])]
                               (recur (finalize-round updated-game-state)))
                             game-state))]
    (finalize-game final-game-state)))
