(ns battlebots.handlers.routing
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]))

(re-frame/register-handler
  :update-word
  (fn [db [_ word]]
    (assoc db :word word)))
