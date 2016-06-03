(ns battlebots.db
  (:require [battlebots.services.utils :refer [get-item]]))

(def default-db
  {:bootstrapping? false
   :auth-token (get-item "token")
   :user nil
   :users []
   :active-game {}
   :active-panel nil
   :active-modal nil
   :active-alert nil
   :games []
   :errors []})
