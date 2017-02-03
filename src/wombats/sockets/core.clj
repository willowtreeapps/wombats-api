(ns wombats.sockets.core
  "Core library for working with wombat socket channels"
  (:require [clojure.core.async :as a :refer [put! <! timeout]]
            [clojure.edn :as edn]))

;; Helpers

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
  [ws-atom chan-id message]
  (let [chan (get-in @ws-atom [chan-id :chan])]
    (put! chan (format-message message))))

;; Handlers

(defn socket-error
  "Called when there has been an error"
  [t]
  (prn (str "WS Error " (.getMessage t))))

(defn socket-close
  "Called when a websocket has closed"
  [code reason]
  (prn (str "WS Closed - Code: " code " Reason: " reason)))

(defn- handshake-handler
  "Responsible for adding the channel to the websocket atom"
  [ws-atom]
  (fn [chan-id msg]
    (swap! ws-atom assoc-in [chan-id :metadata] msg)))

(defn- keep-alive
  "Jetty's WS server has an idle timeout. This handler supports keeping
  that connection open"
  [chan-id msg])

(defn- get-socket-user
  [ws-atom chan-id]
  (-> (get-in @ws-atom [chan-id :metadata :user])
      (assoc :chan-id chan-id)
      (select-keys [:user/id
                    :user/github-username
                    :user/avatar-url
                    :chan-id])))

(defn create-socket-handler-map
  "Allows for adding custom handlers that respond to namespaced messages
  emitted from the ws channel"
  [handler-map ws-atom]
  (fn [raw-msg]
    (let [msg (parse-message raw-msg)
          {:keys [chan-id msg-type]} (:meta msg)
          socket-user (get-socket-user ws-atom chan-id)
          msg-payload (get msg :payload {})
          msg-fn (msg-type (merge {:handshake (handshake-handler ws-atom)
                                   :keep-alive keep-alive}
                                  handler-map))]

      ;; Log in dev mode
      (println "\n---------- Start Client Message ----------")
      (clojure.pprint/pprint msg)
      (println "------------ End Client Message ----------\n\n")

      (msg-fn socket-user msg-payload))))

(defn- remove-chan
  [ws-atom chan-id]
  (prn (str "remove-chan" chan-id)))

(defn new-ws-connection
  [ws-atom datomic]
  (fn [ws-session send-ch]
    (let [chan-id (.hashCode ws-session)]
      (prn (str "Connection " chan-id " establised"))

      ;; Poll for closed socket
      ;; (go-loop [is-closed? (.closed? send-ch)]
      ;;   (<! (timeout 5000))
      ;;   (prn is-closed?)
      ;;   (if is-closed?
      ;;     (remove-chan ws-atom chan-id)
      ;;     (recur (.closed? send-ch))))

      (swap! ws-atom assoc chan-id {:session ws-session
                                    :chan send-ch
                                    :metadata {}})

      (send-message ws-atom
                    chan-id
                    {:meta {:msg-type :handshake}
                     :payload {:chan-id chan-id}}))))
