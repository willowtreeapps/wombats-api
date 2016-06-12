(ns battlebots.game
  (:require [battlebots.arena :as arena]
            [battlebots.sample-bots.bot-one :as bot-one]))

;;
;; HELPER FUNCTIONS
;;

(defn randomize-players
  "Randomizes player ids"
  [players]
  (shuffle (map #(:_id %) players)))

(defn sort-decisions
  "Sorts player decisions based of of a provided execution-order"
  [decisions execution-order]
  (map #(get-player % decisions) execution-order))

(defn get-player
  "Grabs a player by id"
  [id collection]
  (first (filter #(= (:_id %) id) collection)))

(defn get-player-coords
  [_id arena]

  )

;;
;; DECISION FUNCTIONS
;;
;; Each decision function takes the _id (of the user / bot making the decision,
;; the decision map, and the game-state. Once the decision is applied, the decision
;; function will return a modifed game-state (:dirty-arena)

(defn move-player
  [_id {:keys [metadata] :as decision} {:keys [dirty-arena] :as game-state}]
  (let [player-coords (get-player-coords _id dirty-arena)])
  )

;; GAME STATE UPDATE HELPERS
;;

(defn apply-decision
  "Applies player decision to the current state of the game"
  [game-state {:keys [decision _id]}]
  (let [type (:type decision)
        metadata (:metadata decision)
        updated-game-state (cond
                            (= type "MOVE") (move-player _id decision game-state)
                            (= type "SHOOT" game-state) ;; TODO
                            (= type "HEAL" game-state) ;;
                            :else game-state)]
    game-state))

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
  (map #(merge % {:score 0
                  :bot bot-one/run
                  :saved-state {}}) players))

(defn initialize-game
  "Preps the game"
  [{:keys [initial-arena players] :as game}]
  (merge game {:current-arena initial-arena
               :players (initialize-players players)
               :round 0}))

(defn initialize-new-round
  "Preps game-state for a new round"
  [{:keys [current-arena] :as game-state}]
  (merge game-state {:clean-arena current-arena
                     :dirty-arena current-arena}))

(defn finalize-round
  "Modifies game state to close out a round"
  [{:keys [round] :as game-state}]
  (merge game-state {:round (+ 1 round)}))

;;
;; MAIN GAME LOOP
;;

(defn start-game
  "Starts the game loop"
  [{:keys [players initial-arena] :as game}]
  (let [initial-game-state (initialize-game game)]
    (loop [{:keys [round] :as game-state} initial-game-state]
      (when (< round 50)
        (let [updated-game-state (reduce (fn [game-state update-function]
                                           (update-function game-state))
                                         (initialize-new-round game-state)
                                         [resolve-player-turns])]
          (recur (finalize-round updated-game-state)))))))
