(ns wombats.sockets.game
  (:require [clojure.java.io :as io]
            [clojure.spec :as s]
            [clojure.core.async :refer [put! <! timeout]]
            [clojure.edn :as edn]
            [clj-time.core :as t]
            [clj-time.local :as l]
            [clj-time.format :as f]
            [clj-time.periodic :as p]
            [chime :refer [chime-at]]
            [io.pedestal.http.jetty.websockets :as ws]))

(def ^:private game-rooms (atom {}))

(def ^:private connections (atom {}))

(defn clean-connections
  "Removes all closed connections from state"
  [time]
  (doseq [[chan-id {:keys [session]}] @connections]
    (let [channel-open? (.isOpen session)]
      (when-not channel-open?
        (swap! connections dissoc chan-id)))))

(defn connection-clean-err
  "If the scheduler fails this will be called

  TODO: Send to logs
  "
  [error]
  (prn error))

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

  ;; Remove all closed connections
  (clean-connections 0)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
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

(defn send-message
  [chan-id message]
  (let [chan (get-in @connections [chan-id :chan])]
    (when chan
      (put! chan (format-message message)))))

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
    (let [player ((:get-player-from-game datomic) game-id id)]
      (swap! game-rooms
             assoc-in
             [game-id :players chan-id]
             (assoc socket-user :color (:player/color player))))))

(defn- broadcast-game-message
  [game-id formatted-message]
  (let [channel-ids (get-game-room-channel-ids game-id)]
    (doseq [channel-id channel-ids]
      (send-message channel-id
                    {:meta {:msg-type :chat-message
                            :game-id game-id}
                     :payload formatted-message}))))

(defn- chat-message
  [datomic]
  (fn [{:keys [chan-id user/github-username color] :as socket-user}
      {:keys [game-id message] :as msg}]

    (when (and github-username (not= (count message) 0))
      (let [formatted-message {:username github-username
                               :message message
                               :color (get-player-color game-id chan-id)
                               :timestamp (str (l/local-now))}]
        (broadcast-game-message game-id formatted-message)))))

;; Broadcast functions

(defn broadcast-arena
  [game-id arena]
  (let [channel-ids (get-game-room-channel-ids game-id)]
    (doseq [channel-id channel-ids]
      (send-message channel-id
                    {:meta {:msg-type :frame-update}
                     :payload arena}))))

(defn broadcast-stats
  [game-id stats]
  (let [viewers (get-game-room-channel-ids game-id)]

    (doseq [chan-id viewers]
      (send-message chan-id
                    {:meta {:msg-type :stats-update}
                     :payload stats}))))

(defn create-socket-handler-map
  "Allows for adding custom handlers that respond to namespaced messages
  emitted from the ws channel"
  [handler-map]
  (fn [raw-msg]
    (let [msg (parse-message raw-msg)
          {:keys [chan-id msg-type]} (:meta msg)
          socket-user (get-socket-user chan-id)
          msg-payload (get msg :payload {})
          msg-fn (msg-type handler-map)]

      ;; Log in dev mode
      (println "\n---------- Start Client Message ----------")
      (clojure.pprint/pprint msg)
      (println "------------ End Client Message ----------\n\n")

      (msg-fn socket-user msg-payload))))

(defn- message-handlers
  [datomic]
  {:keep-alive keep-alive
   :handshake (handshake datomic)
   :join-game (join-game datomic)
   :authenticate-user (authenticate-user datomic)
   :chat-message (chat-message datomic)})

(defn new-ws-connection
  [datomic]
  (fn [ws-session send-ch]
    (let [chan-id (.hashCode ws-session)]
      (prn (str "Connection " chan-id " establised"))

      (swap! connections assoc chan-id {:session ws-session
                                        :chan send-ch
                                        :metadata {}})

      (send-message chan-id
                    {:meta {:msg-type :handshake}
                     :payload {:chan-id chan-id}}))))

(defn- socket-error
  "Called when there has been an error"
  [t]
  (prn (str "WS Error " (.getMessage t))))

(defn- socket-close
  "Called when a websocket has closed"
  [code reason]
  (prn (str "WS Closed - Code: " code " Reason: " reason)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PUBLIC FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn in-game-ws
  [datomic]
  {:on-connect (ws/start-ws-connection (new-ws-connection datomic))
   :on-text    (create-socket-handler-map (message-handlers datomic))
   :on-error   socket-error
   :on-close   socket-close})
