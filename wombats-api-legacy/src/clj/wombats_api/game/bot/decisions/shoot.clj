(ns wombats-api.game.bot.decisions.shoot
  (:require [wombats-api.constants.arena :as ac]
            [wombats-api.arena.utils :as au]
            [wombats-api.game.utils :as gu]
            [wombats-api.game.messages :refer [log-shoot-event
                                           log-victim-shot-event]]))

(defn- add-shot-damage
  "Add damage to cells that contain the hp prop"
  [damage]
  (fn [cell]
    (if (:hp cell)
      (assoc cell :hp (- (:hp cell) damage))
      cell)))

(defn- add-shot-metadata
  "Adds the client metadata for shot cells"
  [uuid]
  (fn [cell]
    (assoc-in cell [:md (keyword uuid)] {:type :shot
                                         :decay 1})))

(defn- replace-destroyed-cell
  "Replaces cells that are destroyed with an open space"
  [shot-uuid]
  (fn [cell]
    (let [destructible? (ac/destructible? (:type cell) ac/shot-settings)
          destroyed? (when destructible? (<= (:hp cell) 0))
          updated-md (when destroyed?
                       (assoc (:md cell) (keyword shot-uuid) {:type :destroyed
                                                              :decay 1}))]
      (if destroyed?
        (assoc (:open ac/arena-key) :md updated-md)
        cell))))

(defn- resolve-shot-cell
  "Aggregation pipeline for resolving what should happen to a cell when a shot enters it's space"
  [cell-at-point damage shot-uuid]
  (-> cell-at-point
      ((add-shot-damage damage))
      ((add-shot-metadata shot-uuid))
      ((replace-destroyed-cell shot-uuid))))

(defn- shot-should-progress?
  "Returns a boolean indicating if a shot should continue down it's path"
  [should-progress? cell-at-point hp]
  (boolean (and should-progress?
                (> hp 0)
                (ac/can-occupy? (:type cell-at-point) ac/shot-settings))))

(defn- update-arena
  [cell-at-point damage shot-uuid point {:keys [dirty-arena] :as game-state}]
  (let [updated-cell (resolve-shot-cell cell-at-point damage shot-uuid)
        updated-dirty-arena (au/update-cell dirty-arena point updated-cell)]
    (assoc game-state :dirty-arena updated-dirty-arena)))

(defn- process-shot
  "Process a cell that a shot passes through"
  [{:keys [game-state shot-damage should-progress?
           shot-uuid shooter-id] :as shoot-state} point]
  (let [{:keys [dirty-arena players]} game-state
        cell-at-point (au/get-item point dirty-arena)]
    (if (shot-should-progress? should-progress? cell-at-point shot-damage)
      (let [cell-hp (:hp cell-at-point)
            remaining-shot-damage (Math/max 0 (- shot-damage (or cell-hp 0)))
            damage (- shot-damage remaining-shot-damage)
            updated-game-state (update-arena cell-at-point damage shot-uuid point game-state)]
        {:game-state updated-game-state
         :shot-damage remaining-shot-damage
         :should-progress? true
         :shot-uuid shot-uuid
         :shooter-id shooter-id})
      (assoc shoot-state :should-progress? false))))

(defn shoot
  "Main shoot function"
  [{:keys [direction] :as metadata}
   {:keys [dirty-arena players] :as game-state}
   {:keys [shot-damage-amount] :as config}
   {:keys [decision-maker decision-maker-coords uuid is-player?]}]
  (if (and decision-maker is-player?)
    (let [shoot-coords (au/draw-line-from-point dirty-arena
                                                decision-maker-coords
                                                direction
                                                (:distance ac/shot-settings))]
      (:game-state (reduce process-shot
                           {:game-state game-state
                            :shot-damage shot-damage-amount
                            :should-progress? true
                            :shot-uuid (au/uuid)
                            :shooter-id uuid} shoot-coords)))
    game-state))
