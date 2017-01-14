(ns wombats.sockets.chat
  (:require [clojure.core.async :refer [put!]]
            [io.pedestal.http.jetty.websockets :as ws]
            [wombats.sockets.core :as ws-core]))

(def ^:private chat-connections (atom {}))

(defn chat-message
  [msg]
  (prn msg))

(defn chat-room-map
  [datomic]
  {:on-connect (ws/start-ws-connection (ws-core/new-ws-connection chat-connections
                                                                  datomic))
   :on-message chat-message
   :on-error   ws-core/socket-error
   :on-close   ws-core/socket-close})
