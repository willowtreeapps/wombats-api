(ns wombats-api.game.bot.decisions.move
  (:require [wombats-api.game.utils :as gu]
            [wombats-api.arena.utils :as au]
            [wombats-api.constants.arena :as ac]
            [wombats-api.game.messages :refer [log-collision-event
                                               log-occupy-space-event]]))

(defn- swap-cells
  "swap-cells is responsible for handling movement. One thing to note, metadata is always attached
  to a cell on the board. Metadata can not be transfered, it will always remain attached to the cell
  it belogs to. swap-cells is responsible to updating a cells contents while ensuring that metadata
  remains in the cell it blongs to."
  [{:keys [cell-one-coords cell-two-coords] :as swap-details}
   {:keys [dirty-arena] :as game-state}]
  (let [cell-one (:cell-one swap-details (au/get-item cell-one-coords dirty-arena))
        cell-two (:cell-two swap-details (au/get-item cell-two-coords dirty-arena))
        cell-one-metadata (:md cell-one {})
        cell-two-metadata (:md cell-two {})
        cell-one-update (assoc cell-two :md cell-one-metadata)
        cell-two-update (assoc cell-one :md cell-two-metadata)]
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
                              ;; TODO This fails when both items are destroyed...
                              #_(swap-cells {:cell-one-coords decision-maker-coords
                                           :cell-one decision-maker-cell
                                           :cell-two-coords collision-coords
                                           :cell-two updated-collision-cell} game-state)
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
      (swap-cells {:cell-one-coords decision-maker-coords
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
