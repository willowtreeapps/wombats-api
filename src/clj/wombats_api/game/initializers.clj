(ns wombats-api.game.initializers
  (:require [wombats-api.services.github :refer [get-bot]]
            [wombats-api.game.utils :as gu]
            [wombats-api.arena.utils :refer [uuid]]))

(defn- update-cell-metadata
  "Cleanes up a cell's metadata over a specified period of time."
  [{:keys [md] :as cell}]
  (assoc cell :md (into {} (map (fn [md-entry]
                                  (let [md-uuid (first md-entry)
                                        md-value (last md-entry)
                                        md-update (assoc md-value :decay (dec (:decay md-value)))]
                                    (if (<= (:decay md-update) 0) nil [md-uuid md-update]))) md))))

(defn- update-volatile-cells
  "currently reverts all volatile cells to open if decay-turns is less than 1"
  [arena]
  (vec (map (fn [row]
              (vec (map #(update-cell-metadata %) row))) arena)))

(defn initialize-players
  "Preps each player map for the game. This player map is different from
  the one that is contained inside of the arena and will contain private data
  including hp, decision logic, and saved state."
  [players]
  (map (fn [{:keys [_id bot-repo] :as player}]
         (merge player {:score 0
                        :bot (get-bot _id bot-repo)
                        :saved-state {}
                        :uuid (uuid)
                        :type "player"})) players))

(defn initialize-arena
  [arena players {{:keys [initial-hp]} :player :as config}]
  (map (fn [row]
         (map (fn [cell]
                (cond
                  (gu/is-player? cell) (assoc (gu/sanitize-player
                                                (gu/get-player (:_id cell)
                                                               players))
                                              :hp initial-hp)
                 :else cell)) row)) arena))

(defn initialize-frame
  "Preps game-state for a new frame"
  [{:keys [clean-arena] :as game-state}]
  (merge game-state {:dirty-arena (update-volatile-cells clean-arena)
                     :messages {}}))

(defn initialize-game
  "Preps the game"
  [{:keys [initial-arena players] :as game-state} config]
  (let [initialized-players (initialize-players players)]
    (-> game-state
        (merge {:clean-arena (initialize-arena initial-arena initialized-players config)
                :frames []
                :round-count 0
                :initiative-order nil
                :players initialized-players
                :messages {}}))))
