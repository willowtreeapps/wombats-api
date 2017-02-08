(ns wombats.game.decisions.smoke
  (:require [wombats.game.decisions.helpers :as dh]
            [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]))

(defn- get-smoked-coords
  "Return a collection of coords that should be smokescreened given a central starting point"
  [coords arena-dims]
  (conj (map #(gu/adjust-coords coords % arena-dims)
             (range 8))
        coords))

(defn- calculate-throw-direction
  "determines the direction smoke should be thrown based off of the
  decision makers orientation and the specified throw direction"
  [decision-maker-orientation throw-direction]
  (case throw-direction
    :forward decision-maker-orientation
    :right (mod (+ decision-maker-orientation 2) 8)
    :backward (mod (+ decision-maker-orientation 4) 8)
    :left (mod (- decision-maker-orientation 2) 8)
    nil))

(defn- get-smoke-metadata
  "determines the metadata to display in the arena"
  [{{smoke-duration :arena/smoke-duration} :arena-config}
   decision-maker-state]
  (let [decision-maker (dh/get-decision-maker-contents decision-maker-state)]
    {:type :smoke
     :owner-id (:uuid decision-maker)
     :color (get decision-maker :color "black")
     :decay smoke-duration}))

(defn- calculate-smoke-origin
  "calculates the origin cell of the smoke"
  [decision-maker-state smoke-direction arena-dimensions]
  (let [decision-maker-coords (dh/get-decision-maker-coords decision-maker-state)
        decision-maker-orientation (dh/get-decision-maker-orientation decision-maker-state)
        decision-maker-contents (dh/get-decision-maker-contents decision-maker-state)
        throw-direction (calculate-throw-direction (gu/orientation-to-direction decision-maker-orientation)
                                                   smoke-direction)]
    (if throw-direction
      (gu/adjust-coords decision-maker-coords
                        throw-direction
                        ;; TODO Pull from config smoke-radius + 1
                        arena-dimensions 2)
      decision-maker-coords)))

(defn- update-arena
  "add the smoke metatdata to the arena"
  [game-state smoke-coords smoke-metadata]

  (reduce (fn [game-state-acc smoke-coord]
            (update-in game-state-acc
                       [:frame :frame/arena]
                       #(au/update-cell-metadata-with %
                                                      smoke-coord
                                                      (fn [existing-metadata]
                                                        (conj existing-metadata
                                                              smoke-metadata)))))
          game-state
          smoke-coords))

(defn- update-player-stats
  "update the players stats"
  [game-state decision-maker-state]

  (let [decision-maker (dh/get-decision-maker-contents decision-maker-state)]
    (if (= (:type decision-maker) :wombat)
      (-> game-state
          (update-in [:players (:uuid decision-maker) :stats]
                     (fn [stats]
                       (update stats :stats/smoke-bombs-thrown inc))))
      game-state)))

(defn smoke
  [game-state
   metadata
   decision-maker-state]

  (let [arena-dimensions (dh/get-arena-dimensions game-state)
        smoke-direction (keyword (or (:direction metadata) "drop"))
        smoke-origin (calculate-smoke-origin decision-maker-state
                                             smoke-direction
                                             arena-dimensions)
        smoke-coords (get-smoked-coords smoke-origin arena-dimensions)
        smoke-metadata (get-smoke-metadata game-state decision-maker-state)]
    (-> game-state
        (update-arena smoke-coords smoke-metadata)
        (update-player-stats decision-maker-state))))
