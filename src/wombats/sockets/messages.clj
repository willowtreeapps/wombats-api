(ns wombats.sockets.messages
  (:require [clojure.edn :as edn]))

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
  "Converts the msg into a string before sending it

  NOTE: As of 1.9.0-alpha14, prn-str automatically converts namespaced keywords into
  namespaced maps.

  https://github.com/clojure/clojure/commit/0b930f1e7e2bb2beef4ed1a12c51e4e17782666d#diff-fffb1aa81bef3f5238a444e036bec5cd

  Currently the cljs reader does not support parsing this format

  http://dev.clojure.org/jira/browse/CLJS-1706

  For now we have to turn of the new feature until cljs adds support"
  [msg] (binding [*print-namespace-maps* false] (prn-str msg)))

(defn- get-message
  [msg-type payload]
  {:meta {:msg-type msg-type}
   :payload payload})

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messages
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn arena-message
  [arena]
  (get-message :frame-update arena))

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
  [{:keys [:game/frame :game/start-time] :as game-state}]
  (if (pre-game-start? game-state)
    start-time
    (:frame/round-start-time frame)))

(defn- get-round-number
  [{:keys [:game/frame] :as game-state}]
  (if (pre-game-start? game-state)
    1
    (:frame/round-number frame)))

(defn- format-winners
  [players]
  (map (fn [player]
         {:username (get-in player [:player/user :user/github-username])
          :wombat-name (get-in player [:player/wombat :wombat/name])
          :wombat-color (:player/color player)
          :score (get-in player [:player/stats :stats/score])}) players))

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
  [{:keys [:game/players] :as game-state}]

  (if (post-game? game-state)
    (-> players
        (format-players)
        (sort-by-stats)
        (filter-winners)
        (format-winners)
        (vec))
    nil))

(defn- sanitize-game-state
  [game-state]
  (-> game-state
      (dissoc :game/initiative-order)
      (dissoc :game/zakano)
      (update :game/players (fn [players]
                              (reduce-kv (fn [player-map player-id player]
                                           (assoc player-map player-id (-> player
                                                                           (dissoc :db/id)
                                                                           (dissoc :user/github-access-token))))
                                         {}
                                         players)))))

(defn game-info-message
  "Pulls out relevant info from game-state and sends it in join-game"
  [game-state]
  (get-message :game-info (sanitize-game-state game-state)))

(defn handshake-message
  [chan-id]
  (get-message :handshake
               {:chan-id chan-id}))

(defn simulation-message
  [game-state]
  (get-message :simulator-update
               game-state))

(defn missing-simulator-template-message
  [template-id]
  (get-message :simulator-error
               {:template-id template-id
                :message "Something went wrong while trying to start the simulation"}))
