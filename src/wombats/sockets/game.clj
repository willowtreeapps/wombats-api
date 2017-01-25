(ns wombats.sockets.game
  (:require [clojure.java.io :as io]
            [clojure.spec :as s]
            [clojure.core.async :refer [put!]]
            [clojure.edn :as edn]
            [io.pedestal.http.jetty.websockets :as ws]
            [wombats.sockets.core :as ws-core]))

(def ^:private game-connections (atom {}))

(s/def ::user-id int?)
(s/def ::game-id int?)
(s/def ::game-handshake (s/keys :req [::user-id ::game-id]))

;; -------------------------------------
;; -------- Dev Simulator --------------
;; -------------------------------------

(defn- get-arena
  [name]
  (edn/read-string (slurp (io/resource (str "arena/" name)))))

(defn- start-simulation
  [chan-id]
  (let [frames [(get-arena "small-1.edn")
                (get-arena "small-2.edn")
                (get-arena "small-3.edn")
                (get-arena "small-4.edn")]]

    (doall (map (fn [frame]
                  (Thread/sleep 500)
                  (ws-core/send-message game-connections
                                        chan-id
                                        {:meta {:msg-type :frame-update}
                                         :payload {:arena frame}}))
                frames))))

;; -------------------------------------
;; -------- Message Handlers -----------
;; -------------------------------------

(defn- command-handler
  [chan-id msg]
  ;; TODO Handle command queue
  )

(defn- handshake-handler
  "Performs the inital game handshake.

  Handshake Steps:

  1. Client - initiate request, passing conn-id, user-token, and game-id
  2. Server - Authenticate user and lookup game
  3. Server - Check user is authorized to join game
  4. Server - Verify conn-id exists & not take. Assign user data to connection
  5. Server - Send frames (in game) / status messages (pre / post game)"
  [datomic]
  (fn [chan-id msg]
    ;; TODO Auth / Game lookup

    (swap! game-connections assoc-in [chan-id :metadata] msg)

    ;; Simulation for dev mode
    (start-simulation chan-id)))

(defn- message-handlers
  [datomic]
  {:handshake (handshake-handler datomic)
   :cmd command-handler})

;; -------------------------------------
;; -------- Public Functions -----------
;; -------------------------------------

(defn in-game-ws
  [datomic]
  {:on-connect (ws/start-ws-connection (ws-core/new-ws-connection game-connections
                                                                  datomic))
   :on-text    (ws-core/create-socket-handler-map (message-handlers datomic)
                                                  game-connections)
   :on-error   ws-core/socket-error
   :on-close   ws-core/socket-close})

;; -------------------------------------
;; -------- Dev Funcitons -------------
;; -------------------------------------

(defn- reset-atom
  "NOTE: Working on clearing out closed soket connections.
  In the time being this will work."
  []
  (reset! game-connections {}))
