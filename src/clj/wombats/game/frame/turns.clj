(ns wombats.game.frame.turns
  (:require [wombats.arena.partial :refer [get-arena-area]]
            [wombats.arena.occlusion :refer [occluded-arena]]
            [wombats.arena.utils :as au]
            [wombats.game.utils :as gu]
            [wombats.game.bot.decisions.move :as move]
            [wombats.game.bot.decisions.save-state :as save]
            [wombats.game.bot.decisions.shoot :as shoot]))

(defn- get-decision-maker-data
  [uuid arena]
  (let [decision-maker-coords (gu/get-item-coords uuid arena)
        decision-maker (when decision-maker-coords
                         (au/get-item decision-maker-coords arena))]
    {:uuid uuid
     :decision-maker-coords decision-maker-coords
     :decision-maker decision-maker
     :is-player? (gu/is-player? decision-maker)}))

(defn- process-command
  "Processes a single command for a given player if the player has enough time for that command."
  [uuid {:keys [command-map] :as config}]
  (fn [{:keys [game-state remaining-time]} command]
    (let [{:keys [cmd metadata]} command
          {:keys [dirty-arena]} game-state
          {:keys [decision-maker] :as decision-maker-data} (get-decision-maker-data uuid dirty-arena)
          time-cost (get-in command-map [(keyword cmd) :tu] 0)
          updated-time (- remaining-time time-cost)
          should-update? (and (>= updated-time 0) decision-maker)
          updated-game-state (if should-update?
                               (condp = cmd
                                 "MOVE"
                                 (move/move metadata game-state config decision-maker-data)

                                 "SHOOT"
                                 (shoot/shoot metadata game-state config decision-maker-data)

                                 "SET_STATE"
                                 (save/save-state metadata game-state decision-maker-data)

                                 game-state)
                               game-state)]
      {:game-state updated-game-state
       :remaining-time (if should-update?
                         updated-time
                         remaining-time)})))

(defn- apply-decisions
  "Applies player or ai decision to the current state of the game"
  [{:keys [initial-time-unit-count] :as config}]
  (fn [game-state {:keys [uuid]
                   {:keys [commands]} :decision}]
    (:game-state (reduce (process-command uuid config)
                         {:game-state game-state
                          :remaining-time initial-time-unit-count}
                         commands))))

(defn- calculate-ai-decision
  [game-parameters]
  (let [command-options [;; [{:cmd  "SHOOT"
                         ;;   :metadata  {:direction (rand-nth  [0 1 2 3 4 5 6 7]) :energy 20}}
                         ;;  {:cmd  "SET_STATE"
                         ;;   :metadata  {:foo  "bar"}}]
                         [{:cmd  "MOVE"
                           :metadata  {:direction (rand-nth  [0 1 2 3 4 5 6 7])}}]]]
    {:commands (rand-nth command-options)}))

(defn- resolve-decisions
  "Returns a vecor of player decisions based off of the logic provided by
   their bots and an identical clean version of the arena"
  [{:keys [clean-arena players] :as game-state}
   config
   initiative-order]
  (remove
   nil?
   (map-indexed
    (fn [idx uuid]
      (let [{:keys [decision-maker-coords
                    decision-maker
                    is-player?]} (get-decision-maker-data uuid clean-arena)]
        (when decision-maker
          (let [player (when is-player?
                         (gu/get-player (:_id decision-maker) players))
                radius (if is-player?
                         (get-in config [:player :partial-arena-radius])
                         (get-in config [:ai :partial-arena-radius]))
                arena (-> clean-arena
                          (get-arena-area decision-maker-coords radius)
                          (occluded-arena decision-maker-coords))
                game-parameters {:arena arena
                                 :state (get (or player {}) :state {})
                                 :uuid uuid
                                 :hp (:hp decision-maker)
                                 :initiative-order idx
                                 :initiative-count (count initiative-order)}]
            {:decision (if is-player?
                         ((load-string (:bot player)) game-parameters)
                         (calculate-ai-decision game-parameters))
             :uuid uuid}))))
    initiative-order)))

(defn resolve-turns
  "Updates the arena by applying each players' movement logic"
  [{:keys [clean-arena initiative-order] :as game-state} config]
  (let [decisions (resolve-decisions game-state config initiative-order)]
    (reduce (apply-decisions config) game-state decisions)))
