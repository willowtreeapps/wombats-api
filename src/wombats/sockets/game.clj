(ns wombats.sockets.game
  (:require [clojure.java.io :as io]
            [clojure.spec :as s]
            [clojure.core.async :refer [put! <! timeout]]
            [clojure.edn :as edn]
            [taoensso.timbre :as log]
            [datomic.api :as d]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [chime :refer [chime-at]]
            [io.pedestal.http.jetty.websockets :as ws]
            [wombats.sockets.messages :as m]
            [wombats.game.initializers :as i]))

(def ^:private game-rooms (atom {}))
(def ^:private connections (atom {}))

(defn- remove-chan-from-room
  [game-id chan-id]
  (swap! game-rooms update-in [game-id :players] dissoc chan-id))

(defn- cleanup-closed-connections
  []
  (doseq [[chan-id {:keys [session]}] @connections]
    (let [channel-open? (.isOpen session)]
      (when-not channel-open?
        ;; Remove channel from game rooms
        (map (fn [game-id {players :players}]
               (let [contains-connection? (chan-id players)]
                 (when contains-connection?
                   (remove-chan-from-room game-id chan-id))))
             @game-rooms)
        ;; Remove channel from connections
        (swap! connections dissoc chan-id)))))

(defn- cleanup-empty-games
  []
  (doseq [[game-id {:keys [players]}] @game-rooms]
    (let [game-empty? (= (count players) 0)]
      (when game-empty?
        (swap! game-rooms dissoc game-id)))))

(defn clean-connections
  "Removes all closed connections from state"
  [time]
  (cleanup-closed-connections)
  (cleanup-empty-games))

(defn connection-clean-err
  "If the scheduler fails this will be called"
  [error]
  (log/error (str "Failed to clean up socket connections: " error)))

(defn start-connection-cleanup
  "Kicks off the cleanup job responsible for removing closed channels
  from state."
  []
  (chime-at
   (rest (p/periodic-seq (t/now) (-> 10 t/seconds)))
   clean-connections
   {:error-handler connection-clean-err}))

(start-connection-cleanup)

(defn dev-socket-helpers
  "Dev functions for easy access to atom state

  If running cider, use C-x C-e to eval the helper functions"
  []

  ;; Print number of connections
  (prn (count (keys @connections)))

  ;; Print connections
  (clojure.pprint/pprint @connections)

  (clojure.pprint/pprint @game-rooms)

  ;; Remove all closed connections
  (clean-connections 0)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-message
  [chan-id message]
  (let [chan (get-in @connections [chan-id :chan])]
    (when chan
      (put! chan (m/format-message message)))))

(defn- get-socket-user
  [chan-id]
  (-> (get-in @connections [chan-id :metadata :user])
      (assoc :chan-id chan-id)
      (select-keys [:user/id
                    :user/github-username
                    :user/avatar-url
                    :chan-id])))

(defn- get-game-room
  [game-id]
  (get @game-rooms game-id))

(defn- get-game-room-players
  [game-id]
  (:players (get-game-room game-id)))

(defn- get-game-room-channel-ids
  [game-id]
  (keys (get-game-room-players game-id)))

(defn- get-game-room-player
  [game-id chan-id]
  (get (get-game-room-players game-id) chan-id))

(defn- get-player-color
  [game-id chan-id]
  (:color (get-game-room-player game-id chan-id)))

;; Broadcast helper functions

(defn- broadcast-to-viewers
  [game-id message]
  (let [channel-ids (get-game-room-channel-ids game-id)]
    (doseq [channel-id channel-ids]
      (send-message channel-id message))))

;; Broadcast/send functions

(defn broadcast-arena
  [{:keys [:game/id :game/frame] :as game-state}]
  (broadcast-to-viewers id (m/arena-message (:frame/arena frame)))
  game-state)

(defn broadcast-game-info
  [{:keys [:game/game-id] :as game-state}]
  (broadcast-to-viewers game-id
                        (m/game-info-message game-state))
  game-state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- keep-alive
  "Jetty's WS server has an idle timeout. This handler supports keeping
  that connection open"
  [chan-id msg])

(defn- handshake
  "Performs the inital game handshake.

  Client - initiate request, passing conn-id"
  [datomic]
  (fn [{:keys [chan-id]} msg]
    (swap! connections assoc-in [chan-id :metadata] msg)))

(defn- authenticate-user
  [datomic]
  (fn [{:keys [chan-id]} msg]
    (let [user ((:get-user-by-access-token datomic)
                (:access-token msg))]
      (swap! connections assoc-in [chan-id :metadata :user] user))))

(defn- join-game
  [datomic]
  (fn [{:keys [chan-id :user/id] :as socket-user}
      {:keys [game-id]}]
    (let [player ((:get-player-from-game datomic) game-id id)
          game-state ((:get-game-state-by-id datomic) game-id)
          arena (get-in game-state [:game/frame :frame/arena])]

      (swap! game-rooms
             assoc-in
             [game-id :players chan-id]
             (assoc socket-user :color (:player/color player)))

      ;; Sends the current game frame to the frontend
      (send-message chan-id
                    (m/arena-message arena))

      ;; Sends the game info to the front end
      (send-message chan-id
                    (m/game-info-message game-state)))))

(defn- leave-game
  [_]
  (fn [{:keys [chan-id] :as socket-user}
      {:keys [game-id]}]
    (remove-chan-from-room game-id chan-id)))

(defn- chat-message
  [datomic]
  (fn [{:keys [chan-id user/github-username color] :as socket-user}
      {:keys [game-id message] :as msg}]

    (when (and github-username (not= (count message) 0))
      (let [formatted-message {:username github-username
                               :message message
                               :color (get-player-color game-id chan-id)
                               :timestamp (->> (t/now)
                                               (format "#inst \"%s\"")
                                               (read-string))}]
        (broadcast-to-viewers game-id
                              (m/chat-message game-id formatted-message))))))

(defn create-socket-handler-map
  "Allows for adding custom handlers that respond to namespaced messages
  emitted from the ws channel"
  [handler-map]
  (fn [raw-msg]
    (let [msg (m/parse-message raw-msg)
          {:keys [chan-id msg-type]} (:meta msg)
          socket-user (get-socket-user chan-id)
          msg-payload (get msg :payload {})
          msg-fn (msg-type handler-map)]

      (when-not (contains? #{:keep-alive
                             :process-simulation-frame} msg-type)

        (log/info
         (str
          "\n---------- Start Client Message ----------\n"
          msg
          "\n------------ End Client Message ----------\n\n")))

      (msg-fn socket-user msg-payload))))

(defn- message-handlers
  [datomic aws-credentials]
  {:keep-alive keep-alive
   :handshake (handshake datomic)
   :join-game (join-game datomic)
   :leave-game (leave-game datomic)
   :authenticate-user (authenticate-user datomic)
   :chat-message (chat-message datomic)})

(defn new-ws-connection
  [datomic]
  (fn [ws-session send-ch]
    (let [chan-id (.hashCode ws-session)]
      (log/info (str "Connection " chan-id " establised"))

      (swap! connections assoc chan-id {:session ws-session
                                        :chan send-ch
                                        :metadata {}})

      (send-message chan-id
                    (m/handshake-message chan-id)))))

(defn- socket-error
  "Called when there has been an error"
  [t]
  (log/error (str "WS Error " (.getMessage t))))

(defn- socket-close
  "Called when a websocket has closed"
  [code reason]
  (log/info (str "WS Closed - Code: " code " Reason: " reason)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PUBLIC FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn in-game-ws
  [datomic aws-credentials]
  {:on-connect (ws/start-ws-connection (new-ws-connection datomic))
   :on-text    (create-socket-handler-map (message-handlers datomic aws-credentials))
   :on-error   socket-error
   :on-close   socket-close})
