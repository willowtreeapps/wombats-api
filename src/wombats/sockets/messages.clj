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

(defn game-info-message
  "Pulls out relevant info from game-state and sends it in join-game"
  [game]
  (get-message :game-info
               {:start-time (:game/start-time game)
                :max-players (:game/max-players game)
                :name (:game/name game)
                :status (:game/status game)}))

(defn stats-message
  [stats]
  (get-message :stats-update
               (vec stats)))
