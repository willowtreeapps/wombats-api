(ns battlebots.handlers
  (:require [re-frame.core :as re-frame :refer [register-handler]]
            [battlebots.db :as db]))

(register-handler
 :initialize-db
 (fn [_ _]
   db/default-db))

(register-handler
 :update-word
 (fn [db [_ word]]
   (assoc db :word word)))
