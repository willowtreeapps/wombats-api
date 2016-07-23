(ns wombats.handlers.routing
    (:require [re-frame.core :as re-frame]
              [wombats.db :as db]))

(defn set-active-panel
  "sets the active view"
  [db [_ active-panel]]
    (assoc db :active-panel active-panel))

(re-frame/register-handler :set-active-panel set-active-panel)
