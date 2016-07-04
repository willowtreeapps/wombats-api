(ns battlebots.game
  (:require [battlebots.services.mongodb :as db]
            [battlebots.services.github :as github]
            [battlebots.constants.arena :refer [arena-key]]
            [battlebots.arena.occlusion :refer [occluded-arena]]
            [battlebots.arena.partial :refer [get-arena-area]]
            [battlebots.constants.game :refer [segment-length
                                               game-length
                                               collision-damage-amount
                                               command-map
                                               initial-time-unit-count
                                               partial-arena-radius]]
            [battlebots.arena.utils :as au]))

;;
;; HELPER FUNCTIONS
;;

;; Modified from
;; http://stackoverflow.com/questions/4830900/how-do-i-find-the-index-of-an-item-in-a-vector
(defn- position
  [pred coll]
  (first (keep-indexed (fn [idx x]
                         (when (pred x)
                           idx))
                       coll)))

(defn- is-player?
  "Checks to see if an item is a player"
  [item]
  (boolean (:login item)))

(defn- update-player-with
  "updates a private player object"
  [player-id players update]
  (map #(if (= player-id (:_id %))
          (merge % update)
          %) players))

(defn- total-rounds
  "Calculates the total number of rounds that have elapsed"
  [num-rounds num-segments]
  (+ (* num-segments segment-length) num-rounds))

(defn- get-player
  "Grabs a player by id from the private player collection"
  [id collection]
  (first (filter #(= (:_id %) id) collection)))

(defn- sanitize-player
  "Sanitizes the full player object returning the partial used on the game map"
  [player]
  (select-keys player [:_id :login :energy]))

(defn- save-segment
  [{:keys [_id players rounds segment-count] :as game-state}]
  (db/save-game-segment {:game-id _id
                         :players (map sanitize-player players)
                         :rounds rounds
                         :segment segment-count}))

(defn- randomize-players
  "Randomizes player ids"
  [players]
  (shuffle (map #(:_id %) players)))

(defn- sort-decisions
  "Sorts player decisions based of of a provided execution-order"
  [decisions execution-order]
  (map #(get-player % decisions) execution-order))

(defn- get-player-coords
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
                            :coords [idx row-number]}
                           {:row (+ 1 row-number)}))))
                   {:row 0} arena)))

(defn- can-occupy-space?
  "determins if a bot can occupy a given space"
  [{:keys [type] :as space}]
  (boolean (or
            (= type "open")
            (= type "food")
            (= type "poison"))))

(defn- determine-effects
  "Determines how player stats should be effected"
  [{:keys [type] :as space}]
  (cond
   (= type "open") {}
   (= type "food")  {:energy #(+ % 10)}
   (= type "poison") {:energy #(- % 5)}))

(defn- apply-player-update
  "Applies an update to a player object"
  [player update]
  (reduce (fn [player [prop update-fn]]
            (assoc player prop (update-fn (get player prop)))) player update))

(defn- modify-player-stats
  "maps over all players and applies an update if the pred matches"
  [player-id update players]
  (map (fn [{:keys [_id] :as player}]
         (if (= player-id _id)
           (apply-player-update player update)
           player)) players))

(defn- apply-damage
  "applies damage to items that have energy. If the item does not have energy, return the item.
  If the item after receiving damage has 0 or less energy, replace it with an open space"
  ([item damage] (apply-damage item damage true))
  ([{:keys [energy] :as item} damage replace-item?]
   (if energy
     (let [updated-energy (- energy damage)
           updated-item (assoc item :energy updated-energy)
           destroyed? (>= 0 updated-energy)]
       (if (and destroyed? replace-item?)
         (:open arena-key)
         updated-item))
     item)))

(defn- get-bot
  "Returns the code a bot executes"
  [player-id repo]
  (let [{:keys [bots access-token] :as player} (db/get-player-with-token player-id)
        {:keys [contents-url] :as bot} (reduce (fn [memo bot]
                                                 (if (= (:repo bot) repo)
                                                   bot
                                                   memo)) nil bots)]
    (github/get-bot-code access-token contents-url)))

;;
;; DECISION EFFECT FUNCTION
;;
;; These functions operate on the game-state and player objects to apply effects

(defn- player-occupy-space
  [coords player-id]
  (fn [{:keys [dirty-arena players] :as game-state}]
     (let [cell-contents (au/get-item coords dirty-arena)
           player (get-player player-id players)
           updated-arena (au/update-cell dirty-arena coords (sanitize-player player))
           player-update (determine-effects cell-contents)
           updated-players (modify-player-stats player-id player-update players)]
       (merge game-state {:dirty-arena updated-arena
                          :players updated-players}))))

(defn- clear-space
  [coords]
  (fn [{:keys [dirty-arena] :as game-state}]
     (let [updated-arena (au/update-cell dirty-arena coords (:open arena-key))]
       (merge game-state {:dirty-arena updated-arena}))))

(defn- apply-collision-damage
  "Apply collision damage is responsible for updating the game-state with applied collision damage.
  Bots that run into item spaces that cannot be occupied will receive damage. If the item that is
  collided with has energy, it to will receive damage. If the item collided with has an energy level
  of 0 or less after the collision, that item will disappear and the bot will occupy its space."
  [player-id player-coords collision-item collision-coords collision-damage]
  (fn [{:keys [players dirty-arena] :as game-state}]
    (if (is-player? collision-item)
      (let [player (get-player player-id players)
            updated-player (apply-damage player collision-damage false)
            victim-id (:_id collision-item)
            victim (get-player victim-id players)
            updated-victim (apply-damage victim collision-damage false)
            update-players-one (update-player-with player-id players updated-player)
            update-players-two (update-player-with victim-id update-players-one updated-victim)]
        (merge game-state {:players update-players-two}))
      (let [player (get-player player-id players)
            updated-player (apply-damage player collision-damage false)
            updated-players (update-player-with player-id players updated-player)
            updated-collision-item (apply-damage collision-item collision-damage)
            collision-item-now-open? (= (:type updated-collision-item) "open")]
        (if collision-item-now-open?
          (merge game-state {:players updated-players
                             :dirty-arena (au/update-cell (au/update-cell dirty-arena
                                                                          collision-coords
                                                                          (sanitize-player updated-player))
                                                          player-coords
                                                          (:open arena-key))})
          (merge game-state {:players updated-players
                             :dirty-arena (au/update-cell (au/update-cell dirty-arena
                                                                          collision-coords
                                                                          updated-collision-item)
                                                          player-coords
                                                          (sanitize-player updated-player))}))))))

;;
;; DECISION FUNCTIONS
;;
;; Each decision function takes the _id (of the user / bot making the decision,
;; the decision map, and the game-state. Once the decision is applied, the decision
;; function will return a modifed game-state (:dirty-arena)

(defn- move-player
  "Determine if a player can move to the space they have requested, if they can then update
  the board by moving the player and apply any possible consequences of the move to the player."
  [player-id {:keys [direction] :as metadata} {:keys [dirty-arena players] :as game-state}]
  (let [player-coords (get-player-coords player-id dirty-arena)
        dimensions (au/get-arena-dimensions dirty-arena)
        desired-coords (au/adjust-coords player-coords direction dimensions)
        desired-space-contents (au/get-item desired-coords dirty-arena)
        take-space? (can-occupy-space? desired-space-contents)]
    (if take-space?
      (reduce #(%2 %1) game-state [(clear-space player-coords)
                                   (player-occupy-space desired-coords player-id)])
      (reduce #(%2 %1) game-state [(apply-collision-damage player-id
                                                           player-coords
                                                           desired-space-contents
                                                           desired-coords
                                                           collision-damage-amount)]))))


(defn- set-player-state
  "Player state is player described. This means players can choose to save any key
  value data they choose. The state will persist and will be sent back to the player
  each turn. A play can update state as much as they would like"
  [player-id metadata {:keys [players] :as game-state}]
  (let [updated-players (map #(if (= (:_id %) player-id)
                                (assoc % :saved-state metadata)
                                %) players)]
    (assoc game-state :players updated-players)))
;;
;; GAME STATE UPDATE HELPERS
;;

(defn- process-command
  "Processes a single command for a given player if the player has enough time for that command."
  [player-id command-map]
  (fn [{:keys [game-state remaining-time]} command]
    (let [{:keys [cmd metadata]} command
          time-cost (or (get-in command-map [(keyword cmd) :tu]) 0)
          updated-time (- remaining-time time-cost)
          should-update? (>= updated-time 0)
          updated-game-state (if should-update?
                               (cond
                                (= cmd "MOVE")
                                (move-player player-id metadata game-state)

                                ;; TODO
                                ;; https://github.com/willowtreeapps/battlebots/issues/45
                                (= cmd "SHOOT")
                                game-state

                                (= cmd "SET_STATE")
                                (set-player-state player-id metadata game-state)

                                :else game-state)
                               game-state)]
      {:game-state updated-game-state
       :remaining-time (if should-update?
                         updated-time
                         remaining-time)})))

(defn- apply-decisions
  "Applies player decision to the current state of the game"
  [command-map]
  (fn [game-state {:keys [decision _id] :as player}]
    (let [{:keys [commands]} decision]
      (:game-state (reduce (process-command _id command-map) {:game-state game-state
                                                              :remaining-time initial-time-unit-count} commands)))))

(defn- resolve-player-decisions
  "Returns a vecor of player decisions based off of the logic provided by
  their bots and an identical clean version of the arena"
  [players clean-arena]
  (map (fn [{:keys [_id bot saved-state energy] :as player}]
         (let [partial-arena (get-arena-area
                              clean-arena
                              (get-player-coords _id clean-arena)
                              partial-arena-radius)]
           {:decision ((load-string bot)
                       {:arena (occluded-arena
                                partial-arena
                                (get-player-coords _id partial-arena))
                        :saved-state saved-state
                        :bot-id _id
                        :energy energy
                        :spawn-bot? false})
            :_id _id})) players))

;;
;; GAME STATE UPDATERS
;;
;; Each one of these functions takes a game-state map and returns
;; a game-state map
;;

(defn- resolve-player-turns
  "Updates the arena by applying each players movement logic"
  [{:keys [players clean-arena] :as game-state}]
  (let [execution-order (randomize-players players)
        player-decisions (resolve-player-decisions players clean-arena)
        sorted-player-decisions (sort-decisions player-decisions execution-order)
        updated-game-state (reduce (apply-decisions command-map) game-state sorted-player-decisions)]
    updated-game-state))

;;
;; GAME INITIALIZERS / FINALIZERS
;;
;; Game initializers are intended to provide additional information to
;; not stored in the game object
;;

(defn- initialize-players
  "Preps each player map for the game. This player map is different from
  the one that is contained inside of the arena and will contain private data
  including energy, decision logic, and saved state."
  [players]
  (map (fn [{:keys [_id bot-repo] :as player}] (merge player {:energy 100
                                                              :bot (get-bot _id bot-repo)
                                                              :saved-state {}})) players))

(defn- initialize-game
  "Preps the game"
  [{:keys [initial-arena players] :as game}]
  (merge game {:clean-arena initial-arena
               :rounds []
               :segment-count 0
               :players (initialize-players players)}))

(defn- initialize-new-round
  "Preps game-state for a new round"
  [{:keys [clean-arena] :as game-state}]
  (merge game-state {:dirty-arena clean-arena}))

(defn- finalize-segment
  "Batches a segment of rounds together, persists them, and returns a clean segment"
  [{:keys [segment-count] :as game-state}]
  (save-segment game-state)
  (merge game-state {:segment-count (inc segment-count)
                     :rounds []}))

(defn- finalize-round
  "Modifies game state to close out a round"
  [{:keys [rounds dirty-arena players] :as game-state}]
  (let [formatted-round {:map dirty-arena
                         :players (map sanitize-player players)}
        updated-game-state (merge game-state {:rounds (conj rounds formatted-round)
                                              :clean-arena dirty-arena})]
    (if (= (count (:rounds updated-game-state)) segment-length)
      (finalize-segment updated-game-state)
      updated-game-state)))

(defn- finalize-game
  "Finializes game"
  [{:keys [players] :as game-state}]
  (save-segment game-state)
  (merge (dissoc game-state
                 :clean-arena
                 :dirty-arena
                 :rounds
                 :segment-count) {:state "finalized"
                                  :players (map sanitize-player players)}))

;;
;; MAIN GAME LOOP
;;

(defn- game-loop
  [initial-game-state]
  (loop [{:keys [rounds segment-count] :as game-state} initial-game-state]
    (if (< (total-rounds (count rounds) segment-count) game-length)
      (let [updated-game-state (reduce (fn [game-state update-function]
                                         (update-function game-state))
                                       (initialize-new-round game-state)
                                       [resolve-player-turns])]
        (recur (finalize-round updated-game-state)))
      game-state)))

(defn start-game
  "Starts the game loop"
  [{:keys [players initial-arena] :as game}]
  (let [initial-game-state (initialize-game game)
        final-game-state (game-loop initial-game-state)]
    (finalize-game final-game-state)))
