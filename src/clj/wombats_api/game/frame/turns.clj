(ns wombats-api.game.frame.turns
  (:require [wombats-api.arena.partial :refer [get-arena-area]]
            [wombats-api.arena.occlusion :refer [occluded-arena]]
            [wombats-api.arena.utils :as au]
            [wombats-api.game.utils :as gu]
            [wombats-api.game.bot.decisions.move :as move]
            [wombats-api.game.bot.decisions.save-state :as save]
            [wombats-api.game.bot.decisions.shoot :as shoot]
            [wombats-api.game.bot.decisions.smokescreen :as smoke]
            [clojail.core :refer [sandbox]]
            [clojail.testers :refer [secure-tester]]))

(def ^:private sb (sandbox secure-tester :timeout 5000))

(defn- get-decision-maker-data
  [uuid arena]
  (let [decision-maker-coords (gu/get-item-coords uuid arena)
        decision-maker (when decision-maker-coords
                         (au/get-item decision-maker-coords arena))]
    {:uuid uuid
     :decision-maker-coords decision-maker-coords
     :decision-maker decision-maker
     :is-player? (gu/is-player? decision-maker)}))

(defn- update-mini-map
  "Attaches a decision makers mini-map to game-state"
  [game-state uuid mini-map]
  (assoc-in game-state [:mini-maps (keyword uuid)] mini-map))

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

                                 "SMOKESCREEN"
                                 (smoke/smokescreen game-state config decision-maker-data)

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
                   {:keys [commands]} :decision
                   mini-map :mini-map}]
    (let [updated-game-state (:game-state (reduce (process-command uuid config)
                                                  {:game-state game-state
                                                   :remaining-time initial-time-unit-count}
                                                  commands))]
      (update-mini-map updated-game-state uuid mini-map))))

(defn- calculate-ai-decision
  "TODO This should pull from another location. For the time being, ai's will move in random directions."
  [game-parameters]
  (let [command-options [[{:cmd  "MOVE"
                           :metadata  {:direction (rand-nth  [0 1 2 3 4 5 6 7])}}]]]
    {:commands (rand-nth command-options)}))

(defn- calculate-player-decision
  "Calculates a players decision"
  [game-parameters {:keys [bot]}]
  ;; TODO figure out how to run the clojure code in a jailed env
  ((load-string bot) game-parameters))

(defn- get-scoped-view
  [arena is-player? coords config]
  (let [radius (if is-player?
                 (get-in config [:player :partial-arena-radius])
                 (get-in config [:ai :partial-arena-radius]))
        center [radius radius]]
    (-> arena
        (get-arena-area coords radius)
        (occluded-arena center))))

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
                arena (get-scoped-view clean-arena
                                       is-player?
                                       decision-maker-coords
                                       config)
                game-parameters {:arena arena
                                 :state (get (or player {}) :state {})
                                 :uuid uuid
                                 :hp (:hp decision-maker)
                                 :initiative-order idx
                                 :initiative-count (count initiative-order)}]
            {:decision (if is-player?
                         (calculate-player-decision game-parameters player)
                         (calculate-ai-decision game-parameters))
             :uuid uuid
             :mini-map arena}))))
    initiative-order)))

(defn resolve-turns
  "Updates the arena by applying each players' movement logic"
  [{:keys [clean-arena initiative-order] :as game-state} config]
  (let [decisions (resolve-decisions game-state config initiative-order)]
    (reduce (apply-decisions config) game-state decisions)))
