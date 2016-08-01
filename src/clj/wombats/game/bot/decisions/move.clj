(ns wombats.game.bot.decisions.move
  (:require [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]
            [wombats.constants.arena :as ac]
            [wombats.game.messages :refer [log-collision-event
                                           log-occupy-space-event]]))

(defn- apply-player-on-player-damage
  "Applies damage to players when they collide with each other"
  [{:keys [player-id player-coords
           updated-player collision-item
           collision-damage game-state
           players]}]
  (let [victim-id (:_id collision-item)
        victim (gu/get-player victim-id players)
        updated-victim (gu/apply-damage victim collision-damage false)
        update-players-one (gu/update-player-with player-id players updated-player)
        update-players-two (gu/update-player-with victim-id update-players-one updated-victim)]
    (merge game-state {:players update-players-two})))

(defn- apply-player-on-object-damage
  "Applies damage to players and the object they collide with"
  [{:keys [player-id player-coords
           updated-player collision-item
           collision-damage collision-coords
           game-state players
           dirty-arena]}]
  (let [updated-players (gu/update-player-with player-id players updated-player)
        updated-collision-item (gu/apply-damage collision-item collision-damage)
        collision-item-now-open? (= (:type updated-collision-item) "open")]
    (if collision-item-now-open?
      (merge game-state {:players updated-players
                         :dirty-arena (au/update-cell
                                       (au/update-cell dirty-arena
                                                       collision-coords
                                                       (gu/sanitize-player updated-player))
                                       player-coords
                                       (:open ac/arena-key))})
      (merge game-state {:players updated-players
                         :dirty-arena (au/update-cell
                                       (au/update-cell dirty-arena
                                                       collision-coords
                                                       updated-collision-item)
                                       player-coords
                                       (gu/sanitize-player updated-player))}))))

(defn- apply-collision-damage
  "Apply collision damage is responsible for updating the game-state with applied collision damage.
  decision makers that run into item spaces that cannot be occupied will receive damage.
  If the item that is collided with has hp, it to will receive damage. If the item collided with
  has an hp level of 0 or less after the collision, that item will disappear and the decision
  maker will occupy its space."
  [{:keys [decision-maker-coords decision-maker is-player?]}
   collision-item collision-coords collision-damage
   {:keys [players dirty-arena] :as game-state}]
  (let [player-id (when is-player? (:_id decision-maker))
        updated-players (if is-player?
                          (gu/modify-player-stats player-id {:hp #(- % collision-damage)} players)
                          players)
        updated-cell (if is-player?
                       (gu/sanitize-player (gu/get-player player-id updated-players))
                       (gu/apply-damage decision-maker collision-damage))
        updated-collision-cell (gu/apply-damage collision-item collision-damage)
        updated-dirty-arean (if (gu/is-open? updated-collision-cell)
                              (-> dirty-arena
                                  (au/update-cell collision-coords updated-cell)
                                  (au/update-cell decision-maker-coords updated-collision-cell))
                              (-> dirty-arena
                                  (au/update-cell decision-maker-coords updated-cell)
                                  (au/update-cell collision-coords updated-collision-cell)))]
    (merge game-state {:players updated-players
                       :dirty-arena updated-dirty-arean})))

(defn- clear-space
  "Returns a function that will clear a given space when passed game-state"
  [coords {:keys [dirty-arena] :as game-state}]
  (let [updated-arena (au/update-cell dirty-arena coords (:open ac/arena-key))]
    (merge game-state {:dirty-arena updated-arena})))

(defn- occupy-space
  "Updates the game state by applying a vaild move to the dirty arena"
  [coords cell-contents decision-maker {:keys [dirty-arena players] :as game-state}]
  (let [is-player? (gu/is-player? decision-maker)
        player-id (when is-player? (:_id decision-maker))
        update-function (ac/determine-effects (:type cell-contents) ac/move-settings)
        updated-players (if is-player?
                          (gu/modify-player-stats player-id update-function players)
                          players)
        updated-cell (if is-player?
                       (gu/sanitize-player (gu/get-player player-id players))
                       (gu/update-with decision-maker update-function))
        updated-arena (au/update-cell dirty-arena coords updated-cell)]
    (-> game-state
        (merge {:dirty-arena updated-arena
                :players updated-players})
        (log-occupy-space-event cell-contents update-function decision-maker))))

(defn move
  "Determine if a decision maker can move to the space they have requested, if they can then update
  the board by moving the decision maker and apply any possible consequences."
  [{:keys [direction] :as metadata}
   {:keys [dirty-arena players] :as game-state}
   {:keys [collision-damage-amount]}
   {:keys [decision-maker-coords
           decision-maker uuid] :as decision-maker-data}]
  (let [dimensions (au/get-arena-dimensions dirty-arena)
        desired-coords (au/adjust-coords decision-maker-coords direction dimensions)
        desired-space-contents (au/get-item desired-coords dirty-arena)
        take-space? (ac/can-occupy? (:type desired-space-contents) ac/move-settings)]
    (if take-space?
      (->> game-state
           (clear-space decision-maker-coords)
           (occupy-space desired-coords desired-space-contents decision-maker))
      (->> game-state
           (apply-collision-damage decision-maker-data
                                   desired-space-contents
                                   desired-coords
                                   collision-damage-amount)
           (log-collision-event uuid
                                desired-space-contents
                                collision-damage-amount)))))
