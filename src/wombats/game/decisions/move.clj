(ns wombats.game.decisions.move
  (:require [wombats.game.decisions.helpers :as dh]
            [wombats.arena.utils :as au]
            [wombats.game.utils :as gu]
            [wombats.constants :refer [game-parameters]]))

(defn- get-desired-space-coords
  "Returns the coords from the move command"
  [[x y] orientation max-x max-y]
  (condp = orientation
    :n [x (mod (dec y) max-y)]
    :e [(mod (inc x) max-x) y]
    :s [x (mod (inc y) max-y)]
    :w [(mod (dec x) max-x) y]
    [x y]))

(defn- can-safely-occupy-space?
  [contents]
  (let [collision-items #{:wood-barrier
                          :steel-barrier
                          :wombat
                          :zakano}]
    (not (contains? collision-items (:type contents)))))

(defn- apply-food-effects
  [decision-maker-contents]
  (update decision-maker-contents :hp #(+ % (:food-hp-bonus game-parameters))))

(defn- apply-poison-effects
  [decision-maker-contents]
  (let [new-hp (- (:hp decision-maker-contents) (:poison-hp-damage game-parameters))]
    ;; If the decision maker does not have enough health remaining, remove from arena
    (if (pos? new-hp)
      (assoc decision-maker-contents :hp new-hp)
      (au/create-new-contents :open))))

(defn update-decision-maker-with
  [decision-maker-contents
   desired-space-contents
   desired-space-metadata]

  (condp = (:type desired-space-contents)
    :food (apply-food-effects decision-maker-contents)
    :poison (apply-poison-effects decision-maker-contents)
    decision-maker-contents))

(defn- update-player-stats
  [game-state decision-maker-contents desired-space-contents]
  (update-in game-state [:game/players
                         (:uuid decision-maker-contents)
                         :player/stats]
             (fn [stats]
               (condp = (:type desired-space-contents)
                 :food (-> stats
                           (update :stats/food-collected inc)
                           (update :stats/score + (:food-score-bonus game-parameters)))
                 :poison (-> stats
                             (update :stats/poison-collected inc))
                 stats))))

(defn- move-into-cell
  [game-state
   {:keys [desired-space-coords
           desired-space-metadata
           desired-space-contents
           decision-maker-coords
           decision-maker-contents]}]
  (let [updated-decision-maker-contents (update-decision-maker-with decision-maker-contents
                                                                    desired-space-contents
                                                                    desired-space-metadata)
        is-player (gu/is-player? decision-maker-contents)
        wombat-died (and is-player
                         (not (gu/is-player? updated-decision-maker-contents)))]
    (-> game-state
        (update-in [:game/frame :frame/arena]
                   #(au/update-cell-contents %
                                             desired-space-coords
                                             updated-decision-maker-contents))
        (update-in [:game/frame :frame/arena]
                   #(au/update-cell-contents %
                                             decision-maker-coords
                                             (au/create-new-contents :open)))
        (cond->
            is-player (update-player-stats decision-maker-contents
                                           desired-space-contents)
            ;; If a wombat died moving (without colliding) it died from poison
            wombat-died (update-in [:game/players
                                    (:uuid decision-maker-contents)
                                    :player/stats]
                                   (fn [stats]
                                     (-> stats
                                         (update :stats/deaths inc)
                                         (update :stats/deaths-by-poison inc))))))))

(defn- apply-collision-damage
  [contents]
  (let [updated-hp (- (:hp contents)
                      (:collision-hp-damage game-parameters))]
    (if (< updated-hp 1)
      (au/create-new-contents :open)
      (assoc contents :hp updated-hp))))

(defn- update-collision-stats
  [game-state
   {collision-type :type}
   {self-type :type
    self-uuid :uuid}]
  (update-in game-state [:game/players self-uuid :player/stats]
             (fn [stats]
               (case collision-type
                 :wombat (update stats :stats/wombat-collisions inc)
                 :zakano (update stats :stats/zakano-collisions inc)
                 :steel-barrier (update stats :stats/steel-barrier-collisions inc)
                 :wood-barrier (update stats :stats/wood-barrier-collisions inc)
                 stats))))

(defn- update-collision-death-stats
  [game-state
   {victim-type :type
    victim-uuid :uuid}
   {collision-type :type}]
  (update-in game-state [:game/players victim-uuid :player/stats]
             (fn [stats]
               (-> stats
                   (update :stats/deaths inc)
                   (update (case collision-type
                             :wombat :stats/deaths-by-wombat-collision
                             :zakano :stats/deaths-by-zakano-collision
                             :steel-barrier :stats/deaths-by-steel-barrier-collision
                             :wood-barrier :stats/deaths-by-wood-barrier-collision)
                           inc)))))

(defn- update-move-stats
  [game-state
   desired-space-contents
   updated-desired-space
   decision-maker-contents
   updated-decision-maker]

  (let [decision-maker-type (:type decision-maker-contents)
        updated-decision-maker-type (:type updated-decision-maker)
        desired-space-type (:type desired-space-contents)
        updated-desired-space-type (:type updated-desired-space)
        decision-maker-is-wombat (= :wombat decision-maker-type)
        desired-space-is-wombat (= :wombat desired-space-type)
        decision-maker-wombat-died (and decision-maker-is-wombat
                                        (not= updated-decision-maker-type :wombat))
        victim-wombat-died (and desired-space-is-wombat
                                (not= updated-desired-space-type :wombat))]
    (cond-> game-state
      ;; Update the decision makers collision stats
      decision-maker-is-wombat
      (update-collision-stats desired-space-contents
                              decision-maker-contents)

      ;; Update the reverse if wombat
      desired-space-is-wombat
      (update-collision-stats decision-maker-contents
                              desired-space-contents)

      ;; Update the decision makers death stats
      decision-maker-wombat-died
      (update-collision-death-stats decision-maker-contents
                                    desired-space-contents)

      ;; Update the victims death stats
      victim-wombat-died
      (update-collision-death-stats desired-space-contents
                                    decision-maker-contents))))

(defn- resolve-collision
  [game-state
   {:keys [desired-space-coords
           desired-space-metadata
           desired-space-contents
           decision-maker-coords
           decision-maker-contents]}]

  (let [updated-desired-space (apply-collision-damage desired-space-contents)
        updated-decision-maker (apply-collision-damage decision-maker-contents)
        take-space (can-safely-occupy-space? updated-desired-space)]

    (cond-> (update-move-stats game-state
                               desired-space-contents
                               updated-desired-space
                               decision-maker-contents
                               updated-decision-maker)
      take-space
      (update-in [:game/frame :frame/arena]
                 #(au/update-cell-contents %
                                           desired-space-coords
                                           updated-decision-maker))
      take-space
      (update-in [:game/frame :frame/arena]
                 #(au/update-cell-contents %
                                           decision-maker-coords
                                           (au/create-new-contents :open)))
      (not take-space)
      (update-in [:game/frame :frame/arena]
                 #(au/update-cell-contents %
                                           desired-space-coords
                                           updated-desired-space))
      (not take-space)
      (update-in [:game/frame :frame/arena]
                 #(au/update-cell-contents %
                                           decision-maker-coords
                                           updated-decision-maker)))))

(defn move
  [game-state
   metadata
   decision-maker-state]

  (let [arena (get-in game-state [:game/frame :frame/arena])
        {width :arena/width
         height :arena/height} (:game/arena game-state)
        decision-maker-coords (dh/get-decision-maker-coords decision-maker-state)
        decision-maker-contents (dh/get-decision-maker-contents decision-maker-state)
        {orientation :orientation} decision-maker-contents
        desired-space-coords (get-desired-space-coords decision-maker-coords orientation width height)
        {desired-space-metadata :meta
         desired-space-contents :contents} (gu/get-item-at-coords desired-space-coords arena)
        move-payload {:desired-space-coords desired-space-coords
                      :desired-space-metadata desired-space-metadata
                      :desired-space-contents desired-space-contents
                      :decision-maker-coords decision-maker-coords
                      :decision-maker-contents decision-maker-contents}]
    (if (can-safely-occupy-space? desired-space-contents)
      (-> game-state
          (move-into-cell move-payload))
      (-> game-state
          (resolve-collision move-payload)))))
