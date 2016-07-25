(ns wombats.game.bot.decisions.shoot
  (:require [wombats.constants.arena :as ac]
            [wombats.arena.utils :as au]
            [wombats.game.utils :as gu]
            [wombats.game.messages :refer [log-shoot-event
                                              log-victim-shot-event]]))

(defn- add-shot-metadata
  [uuid]
  (fn [cell]
    (assoc-in cell [:md (keyword uuid)] {:type :shot
                                         :decay 1})))

(defn- add-shot-damage
  "Add damage to cells that contain the hp prop"
  [damage]
  (fn [cell]
    (if (:hp cell)
      (assoc cell :hp (- (:hp cell) damage))
      cell)))

(defn- replace-destroyed-cell
  [shot-uuid]
  (fn [cell]
    (let [destructible? (ac/destructible? (:type cell) ac/shot-settings)
          destroyed? (if destructible?
                       (<= (:hp cell) 0)
                       false)
          updated-md (when destroyed?
                       (assoc (:md cell) (keyword shot-uuid) {:type :destroyed
                                                              :decay 1}))]
      (if destroyed?
        (assoc (:open ac/arena-key) :md updated-md)
        cell))))

(defn- resolve-shot-cell
  "Aggregation pipeline for resolving what should happen to a cell when a shot enters it's space"
  [cell-at-point damage shot-uuid]
  (reduce (fn [cell update-func]
            (update-func cell)) cell-at-point [(add-shot-damage damage)
                                               (add-shot-metadata shot-uuid)
                                               (replace-destroyed-cell shot-uuid)]))

(defn- shot-should-progress?
  "Returns a boolean indicating if a shot should continue down it's path"
  [should-progress? cell-at-point hp]
  (boolean (and should-progress?
               (> hp 0)
               (ac/can-occupy? (:type cell-at-point) ac/shot-settings))))

(defn- update-victim-hp
  "Updates a victim's hp when shoot"
  [shooter-id cell damage {:keys [players] :as game-state}]
  (if (gu/is-player? cell)
    (-> game-state
        (assoc :players (gu/modify-player-stats
                         (:_id cell)
                         {:hp #(- % damage)}
                         players))
        (log-victim-shot-event (:id cell) shooter-id damage))
    game-state))

(defn- reward-shooter
  "Shooters get rewarded for hitting a cell with hp. How much depends on the cell type."
  [shooter-id cell damage {:keys [players] :as game-state}]
  (let [hit-reward (get-in ac/shot-settings [:hit-reward (keyword (:type cell))] nil)
        update-function (when hit-reward
                          (hit-reward damage))
        updated-players (when update-function
                          (gu/modify-player-stats shooter-id {:hp update-function} players))]
    (if updated-players
      (-> game-state
       (assoc :players updated-players)
       (log-shoot-event cell damage shooter-id))
      game-state)))

(defn- update-arena
  [cell-at-point damage shot-uuid point {:keys [dirty-arena] :as game-state}]
  (let [updated-cell (resolve-shot-cell cell-at-point damage shot-uuid)
        updated-dirty-arena (au/update-cell dirty-arena point updated-cell)]
    (assoc game-state :dirty-arena updated-dirty-arena)))

(defn- update-players
  [cell-at-point damage shooter-id game-state]
  (->> game-state
       (reward-shooter shooter-id cell-at-point damage)
       (update-victim-hp shooter-id cell-at-point damage)))

(defn- process-shot
  "Process a cell that a shot passes through"
  [{:keys [game-state hp should-progress?
           shot-uuid shooter-id] :as shoot-state} point]
  (let [{:keys [dirty-arena players]} game-state
        cell-at-point (au/get-item point dirty-arena)]
    (if (shot-should-progress? should-progress? cell-at-point hp)
      (let [cell-hp (:hp cell-at-point)
            remaining-hp (Math/max 0 (- hp (or cell-hp 0)))
            damage (- hp remaining-hp)
            updated-game-state (->> game-state
                                    (update-arena cell-at-point damage shot-uuid point)
                                    (update-players cell-at-point damage shooter-id))]
        {:game-state updated-game-state
         :hp remaining-hp
         :should-progress? true
         :shot-uuid shot-uuid
         :shooter-id shooter-id})
      (assoc shoot-state :should-progress? false))))

(defn shoot
  "Main shoot function"
  [player-id
   {:keys [direction hp] :as metadata}
   {:keys [dirty-arena players] :as game-state}]
  (let [player-coords (gu/get-player-coords player-id dirty-arena)
        shoot-coords (au/draw-line-from-point dirty-arena
                                              player-coords
                                              direction
                                              (:distance ac/shot-settings))
        players-update-shooter-hp (gu/modify-player-stats
                                   player-id
                                   {:hp #(- % hp)}
                                   players)]
    (:game-state (reduce
                  process-shot
                  {:game-state (assoc game-state
                                 :players players-update-shooter-hp)
                   :hp hp
                   :should-progress? true
                   :shot-uuid (au/uuid)
                   :shooter-id player-id} shoot-coords))))
