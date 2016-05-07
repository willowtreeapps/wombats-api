(ns battlebots.handlers.routing
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]))

(re-frame/register-handler
  :set-active-panel
  (fn [db [_ active-panel]]
    (assoc db :active-panel active-panel)))
