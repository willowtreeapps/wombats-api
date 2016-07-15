(ns battlebots.game.bot.decisions.move-player
  (:require [battlebots.game.utils :as gu]
            [battlebots.arena.utils :as au]
            [battlebots.constants.arena :as ac]
            [battlebots.constants.game :refer [collision-damage-amount]]))

(defn- player-occupy-space
  "Updates the game state by applying a vaild move to the dirty arena"
  [coords player-id]
  (fn [{:keys [dirty-arena players] :as game-state}]
     (let [cell-contents (au/get-item coords dirty-arena)
           player (gu/get-player player-id players)
           player-update (ac/determine-effects (:type cell-contents) ac/move-settings)
           updated-arena (au/update-cell dirty-arena coords (gu/sanitize-player player))
           updated-players (gu/modify-player-stats player-id player-update players)]
       (merge game-state {:dirty-arena updated-arena
                          :players updated-players}))))

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
  Bots that run into item spaces that cannot be occupied will receive damage. If the item that is
  collided with has energy, it to will receive damage. If the item collided with has an energy level
  of 0 or less after the collision, that item will disappear and the bot will occupy its space."
  [player-id player-coords collision-item collision-coords collision-damage]
  (fn [{:keys [players dirty-arena] :as game-state}]
    (let [player (gu/get-player player-id players)
          updated-player (gu/apply-damage player collision-damage false)
          update-parameters {:player-id player-id
                             :player-coords player-coords
                             :players players
                             :updated-player updated-player
                             :collision-item collision-item
                             :collision-coords collision-coords
                             :collision-damage collision-damage
                             :game-state game-state
                             :dirty-arena dirty-arena}]
      (if (gu/is-player? collision-item)
        (apply-player-on-player-damage update-parameters)
        (apply-player-on-object-damage update-parameters)))))

(defn- clear-space
  "Returns a function that will clear a given space when passed game-state"
  [coords]
  (fn [{:keys [dirty-arena] :as game-state}]
     (let [updated-arena (au/update-cell dirty-arena coords (:open ac/arena-key))]
       (merge game-state {:dirty-arena updated-arena}))))

(defn move-player
  "Determine if a player can move to the space they have requested, if they can then update
  the board by moving the player and apply any possible consequences of the move to the player."
  [player-id {:keys [direction] :as metadata} {:keys [dirty-arena players] :as game-state}]
  (let [player-coords (gu/get-player-coords player-id dirty-arena)
        dimensions (au/get-arena-dimensions dirty-arena)
        desired-coords (au/adjust-coords player-coords direction dimensions)
        desired-space-contents (au/get-item desired-coords dirty-arena)
        take-space? (ac/can-occupy? (:type desired-space-contents) ac/move-settings)]
    (if take-space?
      (reduce #(%2 %1) game-state [(clear-space player-coords)
                                   (player-occupy-space desired-coords player-id)])
      (reduce #(%2 %1) game-state [(apply-collision-damage player-id
                                                           player-coords
                                                           desired-space-contents
                                                           desired-coords
                                                           collision-damage-amount)]))))
