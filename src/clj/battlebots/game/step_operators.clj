(ns battlebots.game.step-operators
  (:require [battlebots.constants.game :refer [command-map
                                               initial-time-unit-count
                                               partial-arena-radius]]
            [battlebots.arena.occlusion :refer [occluded-arena]]
            [battlebots.arena.partial :refer [get-arena-area]]
            [battlebots.game.bot-decisions :refer [move
                                                   save-state
                                                   shoot]]
            [battlebots.game.utils :as gu]
            [battlebots.arena.utils :as au]
            [battlebots.constants.game :as gc]
            [battlebots.game.bot-helpers :refer [sort-arena
                                                 within-n-spaces
                                                 get-items-coords]]))

(defn- ai-random-move
  [{:keys [game-state sorted-arena ai-centered-coords ai-arena]}]
  (let [within-one-space (within-n-spaces sorted-arena ai-centered-coords 1)]
    ;; TODO Find an open space for now and take it
    )
  game-state)

(defn- ai-calculated-move
  [{:keys [game-state]}]
  game-state)

(defn- calculate-ai-move
  [ai-coords ai-bot {:keys [clean-arena] :as game-state}]
  (let [ai-partial-arena (get-arena-area
                          clean-arena
                          ai-coords
                          gc/ai-partial-arena-radius)
        ai-centered-coords (get-items-coords ai-bot ai-partial-arena)
        ai-occluded-arena (occluded-arena ai-partial-arena ai-centered-coords)
        sorted-arena (sort-arena ai-occluded-arena)
        ai-parameters {:game-state game-state
                       :sorted-arena sorted-arena
                       :ai-arena ai-occluded-arena
                       :ai-centered-coords ai-centered-coords}
        players (or (:player sorted-arena) [])]
    ;; TODO Remove true statement
    (if (empty? players)
      (ai-random-move ai-parameters)
      (ai-calculated-move ai-parameters))))

(defn- apply-ai-decision
  [{:keys [dirty-arena] :as game-state} ai-uuid]
  (let [ai-coords (gu/get-item-coords ai-uuid dirty-arena)
        ai-bot (au/get-item ai-coords dirty-arena)
        is-current-ai-bot? (= ai-uuid (:uuid ai-bot))]
    (if is-current-ai-bot?
      (calculate-ai-move ai-coords ai-bot game-state)
      game-state)))

(defn- get-ai-bots
  "Returns a vector of all the ai bot uuids"
  [arena]
  (reduce (fn [memo row]
            (reduce (fn [ai-bots cell]
                      (if (= (:type cell) "ai")
                        (conj ai-bots (:uuid cell))
                        ai-bots))
                    memo row))
          [] arena))

(defn- randomize-players
  "Randomizes player ids"
  [players]
  (shuffle (map #(:_id %) players)))

(defn- sort-decisions
  "Sorts player decisions based of of a provided execution-order"
  [decisions execution-order]
  (map #(gu/get-player % decisions) execution-order))

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
                                (move player-id metadata game-state)

                                (= cmd "SHOOT")
                                (shoot player-id metadata game-state)

                                (= cmd "SET_STATE")
                                (save-state player-id metadata game-state)

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
                              (gu/get-player-coords _id clean-arena)
                              partial-arena-radius)]
           {:decision ((load-string bot)
                       {:arena (occluded-arena
                                partial-arena
                                (gu/get-player-coords _id partial-arena))
                        :saved-state saved-state
                        :bot-id _id
                        :energy energy
                        :spawn-bot? false})
            :_id _id})) players))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; STEP OPERATORS
;;
;; Step Operators are a collection of functions
;; that can be applied to a game step and
;; operate on the game-state.
;;
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn resolve-player-turns
  "Updates the arena by applying each players' movement logic"
  [{:keys [players clean-arena] :as game-state}]
  (let [execution-order (randomize-players players)
        player-decisions (resolve-player-decisions players clean-arena)
        sorted-player-decisions (sort-decisions player-decisions execution-order)
        updated-game-state (reduce (apply-decisions command-map) game-state sorted-player-decisions)]
    updated-game-state))

(defn resolve-ai-turns
  "Updates the arena by applying each AIs' movement logic"
  [{:keys [dirty-arena] :as game-state}]
  (let [ai-bots (get-ai-bots dirty-arena)]
    (reduce apply-ai-decision game-state (shuffle ai-bots))))
