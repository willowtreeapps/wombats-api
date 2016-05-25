(ns battlebots.db
  (:require [battlebots.services.utils :refer [get-item]]))

(def default-db
  {:bootstrapping? false
   :auth-token (get-item "token")
   :user nil
   :active-game {}
   :games []
   :errors []
   :word "Aloha"})
