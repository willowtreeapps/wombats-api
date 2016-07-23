(ns wombats.socket-handler
  (:require [taoensso.sente :as sente :refer [cb-success?]]
            [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
            [re-frame.core :as re-frame]
            [goog.string :as gstr]))

(defn nil->str [x] (if (or (undefined? x) (nil? x)) "nil" x))

;; https://github.com/ptaoussanis/encore/blob/master/src/taoensso/encore.cljx#L1509
(defn format [fmt & args]
  (let [fmt (or fmt "")
        args (mapv nil->str args)]
    (apply gstr/format fmt args)))

(defn ->output! [fmt & args]
  (let [msg (apply format fmt args)]
    (timbre/debug msg)))

;; Sente event handlers

(defn event-handler
  "Handles namespaced events"
  [[_ event]]
  (let [[event-name data] event]
    (cond
     (= event-name :game/display-frame) (re-frame/dispatch [:game/display-frame data]))))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (->output! "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (:first-open? ?data)
    (->output! "Channel socket successfully established!: %s" ?data)
    (->output! "Channel socket state change: %s" ?data)))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data event]}]
  ;; NOTE this is useful for debugging, however some of the socket endoints
  ;; are very large and will slow down the app to print the payloads
  ;; (->output! "Push event from server: %s" ?data)

  (event-handler event))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (->output! "Handshake: %s" ?data)))

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! [{:keys [ch-chsk] :as sente-conneciton}]
  (stop-router!)
  (reset! router_
    (sente/start-client-chsk-router! ch-chsk event-msg-handler)))

(defn initialize-sente-router
  [sente-connection]
  (start-router! sente-connection))
