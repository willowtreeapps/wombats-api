(ns wombats.sockets.messages
  (:require [clojure.edn :as edn]
            [wombats.game.player-stats :refer [get-player-stats]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-message
  "Attempts to parse the clent message as EDN"
  [raw-message]
  (try
    (edn/read-string raw-message)
    (catch Exception e (prn (str "Invalid client message: " raw-message)))))

(defn format-message
  "Converts the msg into a string before sending it"
  [msg] (prn-str msg))

(defn- get-message
  [msg-type payload]
  {:meta {:msg-type msg-type}
   :payload payload})

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messages
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn arena-message
  [arena]
  (get-message :frame-update
               arena))

(defn chat-message
  [game-id formatted-message]
  (let [msg (get-message :chat-message formatted-message)]
    ;; Inserts :game-id into :meta
    (assoc msg :meta (assoc (:meta msg)
                            :game-id
                            game-id))))

(defn- pre-game-start?
  [{:keys [:game/status]}]
  (contains? #{:pending-closed :pending-open} status))

(defn- post-game?
  [{:keys [:game/status]}]
  (contains? #{:closed} status))

(defn- get-start-time
  [{:keys [game-config frame] :as game-state}]
  (if (pre-game-start? game-config)
    (:game/start-time game-config)
    (:frame/round-start-time frame)))

(defn- get-round-number
  [{:keys [game-config frame] :as game-state}]
  (if (pre-game-start? game-config)
    1
    (:frame/round-number frame)))

(defn- format-winners
  [players]
  (map (fn [player]
         {:username (get-in player [:user :user/github-username])
          :wombat-name (get-in player [:wombat :wombat/name])
          :wombat-color (get-in player [:player :player/color])
          :score (get-in player [:stats :stats/score])}) players))

(defn- filter-winners
  [sorted-stats]
  (reduce (fn [winners player]
               (if (empty? winners)
                 (conj winners player)
                 (if (= (-> winners
                            (first)
                            (get-in [:stats :stats/score]))
                        (get-in player [:stats :stats/score]))
                   (conj winners player)
                   winners)))
             [] sorted-stats))

(defn- sort-by-stats
  [players]
  (sort-by (fn [player]
             (get-in player [:stats :stats/score]))
           >
           (into [] players)))

(defn- format-players
  [player-map]
  (map (fn [[_ player]]
         player) player-map))

(defn- get-game-winner
  [{:keys [game-config players] :as game-state}]

  (if (post-game? game-config)
    (-> players
        (format-players)
        (sort-by-stats)
        (filter-winners)
        (format-winners)
        (vec))
    nil))

(defn game-info-message
  "Pulls out relevant info from game-state and sends it in join-game"
  [{:keys [game-config] :as game-state}]

  (get-message :game-info
               {:round-start-time (get-start-time game-state)
                :round-number (get-round-number game-state)
                :max-players (:game/max-players game-config)
                :player-count (count (:players game-state))
                :name (:game/name game-config)
                :status (:game/status game-config)
                :game-winner (get-game-winner game-state)
                :stats (vec (get-player-stats game-state))}))

(defn handshake-message
  [chan-id]
  (get-message :handshake
               {:chan-id chan-id}))

(defn initialize-simulation-message
  [game-state]
  (get-message :simulator-update
               game-state))

(defn missing-simulator-template-message
  [template-id]
  (get-message :simulator-error
               {:template-id template-id
                :message "Something went wrong while trying to start the simulation"}))
