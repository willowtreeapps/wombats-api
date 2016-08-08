(ns wombats.game.bot.decisions.move
  (:require [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]
            [wombats.constants.arena :as ac]
            [wombats.game.messages :refer [log-collision-event
                                           log-occupy-space-event]]))

(defn- apply-collision-damage
  "Apply collision damage is responsible for updating the game-state with applied collision damage.
  decision makers that run into item spaces that cannot be occupied will receive damage.
  If the item that is collided with has hp, it to will receive damage. If the item collided with
  has an hp level of 0 or less after the collision, that item will disappear and the decision
  maker will occupy its space."
  [{:keys [decision-maker-coords decision-maker]}
   collision-item collision-coords collision-damage
   {:keys [dirty-arena] :as game-state}]
  (let [decision-maker-cell (gu/apply-damage decision-maker collision-damage)
        updated-collision-cell (gu/apply-damage collision-item collision-damage)
        updated-dirty-arena (if (gu/is-open? updated-collision-cell)
                              (-> dirty-arena
                                  (au/update-cell collision-coords decision-maker-cell)
                                  (au/update-cell decision-maker-coords updated-collision-cell))
                              (-> dirty-arena
                                  (au/update-cell decision-maker-coords decision-maker-cell)
                                  (au/update-cell collision-coords updated-collision-cell)))]
    (assoc game-state :dirty-arena updated-dirty-arena)))

(defn- clear-space
  "Returns a function that will clear a given space when passed game-state"
  [coords {:keys [dirty-arena] :as game-state}]
  (let [updated-arena (au/update-cell dirty-arena coords (:open ac/arena-key))]
    (merge game-state {:dirty-arena updated-arena})))

(defn- occupy-space
  "Updates the game state by applying a vaild move to the dirty arena"
  [coords cell-contents decision-maker {:keys [dirty-arena] :as game-state}]
  (let [update-map (ac/determine-effects (:type cell-contents) ac/move-settings)
        decision-maker-cell (gu/update-with decision-maker update-map)

        updated-arena (au/update-cell dirty-arena coords decision-maker-cell)]
    (-> game-state
        (assoc :dirty-arena updated-arena)
        (log-occupy-space-event cell-contents update-map decision-maker))))

(defn move
  "Determine if a decision maker can move to the space they have requested, if they can then update
  the board by moving the decision maker and apply any possible consequences."
  [{:keys [direction] :as metadata}
   {:keys [dirty-arena] :as game-state}
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
