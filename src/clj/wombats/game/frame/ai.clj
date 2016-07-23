(ns wombats.game.frame.ai
  (:require [wombats.game.bot.helpers :refer [sort-arena
                                                 within-n-spaces
                                                 get-items-coords
                                                 calculate-direction-from-origin]]
            [wombats.arena.partial :refer [get-arena-area]]
            [wombats.arena.occlusion :refer [occluded-arena]]
            [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]
            [wombats.constants.game :as gc]
            [wombats.constants.arena :as ac]))

(defn- ai-random-move
  [{:keys [game-state sorted-arena ai-centered-coords ai-original-coords ai-arena ai-bot]}]
  (let [within-one-space (:1 (within-n-spaces sorted-arena ai-centered-coords 1))
        ;; TODO Update movement logic to move over food / poision
        ;; Find an open space for now and take it
        options (:open within-one-space)
        direction (when options
                    (calculate-direction-from-origin
                     ai-centered-coords
                     (:coords (rand-nth options))))
        dirty-arena (:dirty-arena game-state)]
    (if options
      (merge game-state {:dirty-arena (au/update-cell
                                       (au/update-cell dirty-arena
                                                       (au/adjust-coords ai-original-coords
                                                                         direction
                                                                         (au/get-arena-dimensions dirty-arena))
                                                       ai-bot)
                                       ai-original-coords
                                       (:open ac/arena-key))})
      game-state)))

(defn- ai-calculated-move
  [{:keys [game-state]}]
  game-state)

(defn- calculate-ai-move
  [ai-coords ai-bot {:keys [dirty-arena] :as game-state}]
  (let [ai-partial-arena (get-arena-area
                          dirty-arena
                          ai-coords
                          gc/ai-partial-arena-radius)
        ai-centered-coords (get-items-coords ai-bot ai-partial-arena)
        ai-occluded-arena (occluded-arena ai-partial-arena ai-centered-coords)
        sorted-arena (sort-arena ai-occluded-arena)
        ai-parameters {:game-state game-state
                       :sorted-arena sorted-arena
                       :ai-bot ai-bot
                       :ai-arena ai-occluded-arena
                       :ai-centered-coords ai-centered-coords
                       :ai-original-coords ai-coords}
        players (or (:player sorted-arena) [])]
    (if (empty? players)
      (ai-random-move ai-parameters)
      (ai-calculated-move ai-parameters))))

(defn- apply-ai-decision
  [{:keys [dirty-arena] :as game-state} ai-uuid]
  (let [ai-coords (gu/get-item-coords ai-uuid dirty-arena)
        ai-bot (au/get-item ai-coords dirty-arena)
        is-current-ai-bot? (= ai-uuid (:uuid ai-bot))]
    (if is-current-ai-bot?
      (calculate-ai-move ai-coords ai-bot game-state)
      game-state)))

(defn- get-ai-bots
  "Returns a vector of all the ai bot uuids"
  [arena]
  (reduce (fn [memo row]
            (reduce (fn [ai-bots cell]
                      (if (= (:type cell) "ai")
                        (conj ai-bots (:uuid cell))
                        ai-bots))
                    memo row))
          [] arena))

(defn resolve-ai-turns
  "Updates the arena by applying each AIs' movement logic"
  [{:keys [dirty-arena] :as game-state}]
  (let [ai-bots (get-ai-bots dirty-arena)]
    (reduce apply-ai-decision game-state (shuffle ai-bots))))
