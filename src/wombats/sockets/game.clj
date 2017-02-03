(ns wombats.sockets.game
  (:require [clojure.java.io :as io]
            [clojure.spec :as s]
            [clojure.core.async :refer [put! <! timeout]]
            [clojure.edn :as edn]
            [io.pedestal.http.jetty.websockets :as ws]))

(def ^:private game-rooms (atom {}))

(def ^:private connections (atom {}))

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
    (put! chan (format-message message))))

(defn- get-socket-user
  [chan-id]
  (-> (get-in @connections [chan-id :metadata :user])
      (assoc :chan-id chan-id)
      (select-keys [:user/id
                    :user/github-username
                    :user/avatar-url
                    :chan-id])))

(defn- get-channel-ids
  [game-id]
  (keys (get-in @game-rooms [game-id :players])))

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
  (fn [{:keys [chan-id] :as socket-user}
      {:keys [game-id]}]
    ;; TODO Check for ghost connections
    (swap! game-rooms assoc-in [game-id :players chan-id] socket-user)))

;; Broadcast functions

(defn broadcast-arena
  [game-id arena]
  (let [channel-ids (get-channel-ids game-id)]
    (doseq [channel-id channel-ids] (send-message channel-id
                                                  {:meta {:msg-type :frame-update}
                                                   :payload arena}))))

(defn broadcast-stats
  [game-id stats]
  (let [channel-ids (get-channel-ids game-id)]
    (doseq [channel-id channel-ids] (send-message channel-id
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
   :authenticate-user (authenticate-user datomic)})

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
