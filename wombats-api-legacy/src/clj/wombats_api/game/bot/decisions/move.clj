(ns wombats-api.game.bot.decisions.move
  (:require [wombats-api.game.utils :as gu]
            [wombats-api.arena.utils :as au]
            [wombats-api.constants.arena :as ac]
            [wombats-api.game.messages :refer [log-collision-event]]))

(defn- move-into-cell
  "move-into-cell is responsible for handling movement. One thing to note, metadata is always attached
  to a cell on the board. Metadata can not be transfered, it will always remain attached to the cell
  it belogs to. move-into-cell is responsible to updating a cells contents while ensuring that metadata
  remains in the cell it blongs to."
  [{:keys [cell-one-coords cell-two-coords] :as swap-details}
   {:keys [dirty-arena] :as game-state}]
  (let [cell-one (:cell-one swap-details (au/get-item cell-one-coords dirty-arena))
        cell-two (:cell-two swap-details (au/get-item cell-two-coords dirty-arena))
        cell-one-metadata (:md cell-one {})
        cell-two-metadata (:md cell-two {})
        update-parameters (ac/determine-effects (:type cell-two) ac/move-settings)
        cell-one-update (assoc (:open ac/arena-key) :md cell-one-metadata)
        cell-two-update (assoc (gu/update-with cell-one update-parameters) :md cell-two-metadata)]
    (assoc game-state :dirty-arena (-> dirty-arena
                                       (au/update-cell cell-one-coords cell-one-update)
                                       (au/update-cell cell-two-coords cell-two-update)))))

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
      (move-into-cell {:cell-one-coords decision-maker-coords
                       :cell-one decision-maker
                       :cell-two-coords desired-coords
                       :cell-two desired-space-contents} game-state)
      (->> game-state
           (apply-collision-damage decision-maker-data
                                   desired-space-contents
                                   desired-coords
                                   collision-damage-amount)
           (log-collision-event uuid
                                desired-space-contents
                                collision-damage-amount)))))
