(ns battlebots.game
  (:require [battlebots.arena :as arena]
            [battlebots.services.mongodb :as db]
            [battlebots.services.github :as github]
            [battlebots.constants.arena :refer [arena-key]]
            [battlebots.constants.game :refer [segment-length game-length]]
            [battlebots.utils.arena :refer :all]
            [battlebots.utils.arena :refer :all]))

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

(defn can-occupy-space?
  "determins if a bot can occupy a given space"
  [{:keys [type] :as space}]
  (or
   (= type "open")
   (= type "food")
   (= type "poison")))

(defn determine-effects
  "Determines how player stats should be effected"
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
           player-update (determine-effects cell-contents)
           updated-players (modify-player-stats player-id player-update players)]
       (merge game-state {:dirty-arena updated-arena
                          :players updated-players}))))

(defn clear-space
  [coords]
  (fn [{:keys [dirty-arena] :as game-state}]
     (let [updated-arena (arena/update-cell dirty-arena coords (:open arena-key))]
       (merge game-state {:dirty-arena updated-arena}))))

(defn get-bot
  "Returns the code a bot executes"
  [player-id repo]
  (let [{:keys [bots access-token] :as player} (db/get-player-with-token player-id)
        {:keys [contents-url] :as bot} (reduce (fn [memo bot]
                                                 (if (= (:repo bot) repo)
                                                   bot
                                                   memo)) nil bots)]
    (github/get-bot-code access-token contents-url)))

;;
;; DECISION FUNCTIONS
;;
;; Each decision function takes the _id (of the user / bot making the decision,
;; the decision map, and the game-state. Once the decision is applied, the decision
;; function will return a modifed game-state (:dirty-arena)

(defn move-player
  "Determins if a player can move to the space they have requested, if they can then update
  the board by moving the player and apply any possible consequences of the move to the player."
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

(defn process-command
  "Processes a single command for a given player."
  [player-id]
  (fn [game-state command]
    (let [{:keys [cmd metadata]} command]
      (cond
       (= cmd "MOVE") (move-player player-id metadata game-state)
       (= cmd "SHOOT") game-state ;; TODO
       :else game-state))))

(defn apply-decisions
  "Applies player decision to the current state of the game

  TODO: Implement time unit logic
  https://github.com/willowtreeapps/battlebots/issues/44
  "
  [game-state {:keys [decision _id] :as player}]
  (let [{:keys [commands]} decision]
    (reduce (process-command _id) game-state commands)))

(defn resolve-player-decisions
  "Returns a vecor of player decisions based off of the logic provided by
  their bots and an identical clean version of the arena"
  [players clean-arena]
  (map (fn [{:keys [_id bot saved-state energy] :as player}]
         {:decision ((load-string bot)
                     {:arena (get-arena-area clean-arena (get-player-coords _id) 10)
                                        :state saved-state
                                        :bot_id _id
                                        :energy energy
                                        :spawn_bot? false})
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
        updated-game-state (reduce apply-decisions game-state sorted-player-decisions)]
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
  (map (fn [{:keys [_id bot-repo] :as player}] (merge player {:score 0
                                                              :bot (get-bot _id bot-repo)
                                                              :saved-state {}})) players))

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
