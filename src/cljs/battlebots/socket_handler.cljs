(ns battlebots.socket-handler
  (:require [taoensso.sente :as sente :refer [cb-success?]]
            [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
            [goog.string :as gstr]))

(defn nil->str [x] (if (or (undefined? x) (nil? x)) "nil" x))

;; https://github.com/ptaoussanis/encore/blob/master/src/taoensso/encore.cljx#L1509
(defn format [fmt & args]
  (let [fmt (or fmt "")
        args (mapv nil->str args)]
    (apply gstr/format fmt args)))

(def output-el (.getElementById js/document "output"))
(defn ->output! [fmt & args]
  (let [msg (apply format fmt args)]
    (timbre/debug msg)
    (aset output-el "value" (str "• " (.-value output-el) "\n" msg))
    (aset output-el "scrollTop" (.-scrollHeight output-el))))

;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

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
  [{:as ev-msg :keys [?data]}]
  (->output! "Push event from server: %s" ?data))

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
