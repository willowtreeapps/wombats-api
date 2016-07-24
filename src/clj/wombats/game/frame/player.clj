(ns wombats.game.frame.player
  (:require [wombats.arena.partial :refer [get-arena-area]]
            [wombats.arena.occlusion :refer [occluded-arena]]
            [wombats.game.utils :as gu]
            [wombats.game.bot.decisions.move :as m]
            [wombats.game.bot.decisions.save-state :refer [save-state]]
            [wombats.game.bot.decisions.shoot :refer [shoot]]))

(defn- randomize-players
  "Randomizes player ids"
  [players]
  (shuffle (map #(:_id %) players)))

(defn- rotate-players
  "rotates `n` players to the front of list"
  ([players initiative-order] (rotate-players players 1 initiative-order))
  ([players n initiative-order]
   {:pre [(< n (count players))]}
   (let [cv (count players)
         ids (or initiative-order
                 (randomize-players players))]
     (concat (take-last n ids) (drop-last n ids)))))

(defn- sort-decisions
  "Sorts player decisions based of of a provided execution-order"
  [decisions execution-order]
  (map #(gu/get-player % decisions) execution-order))

(defn- process-command
  "Processes a single command for a given player if the player has enough time for that command."
  [player-id {:keys [command-map] :as config}]
  (fn [{:keys [game-state remaining-time]} command]
    (let [{:keys [cmd metadata]} command
          time-cost (get-in command-map [(keyword cmd) :tu] 0)
          updated-time (- remaining-time time-cost)
          should-update? (>= updated-time 0)
          updated-game-state (if should-update?
                               (cond
                                (= cmd "MOVE")
                                (m/move player-id metadata game-state config)

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
  [{:keys [initial-time-unit-count] :as config}]
  (fn [game-state {:keys [_id]
                  {:keys [commands]} :decision
                  :as player}]
    (:game-state (reduce (process-command _id config)
                         {:game-state game-state
                          :remaining-time initial-time-unit-count}
                         commands))))

(defn- resolve-player-decisions
  "Returns a vecor of player decisions based off of the logic provided by
   their bots and an identical clean version of the arena"
  [players clean-arena {{:keys [partial-arena-radius]} :player}]
  (map-indexed (fn [idx {:keys [_id bot saved-state energy] :as player}]
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
                                :spawn-bot? false
                                :initiative-order idx
                                :wombat-count (count players)})
                    :_id _id})) players))

(defn resolve-turns
  "Updates the arena by applying each players' movement logic"
  [{:keys [players clean-arena initiative-order] :as game-state} config]
  (let [execution-order (rotate-players players initiative-order)
        player-decisions (resolve-player-decisions players clean-arena config)
        sorted-player-decisions (sort-decisions player-decisions execution-order)]
    (assoc (reduce (apply-decisions config)
                   game-state
                   sorted-player-decisions)
           :initiative-order execution-order)))
