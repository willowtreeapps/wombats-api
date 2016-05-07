(ns battlebots.handlers.root
    (:require [re-frame.core :as re-frame]
              [battlebots.db :as db]
              [battlebots.handlers.routing]
              [battlebots.handlers.sample]))

(re-frame/register-handler
  :initialize-db
  (fn  [_ _]
    db/default-db))
