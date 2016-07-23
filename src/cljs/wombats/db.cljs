(ns wombats.db
  (:require [wombats.services.utils :refer [get-item]]))

(def default-db
  {:bootstrapping? false
   :auth-token (get-item "token")
   :user nil
   :users []
   :active-game {}
   :active-panel nil
   :active-modal nil
   :active-alert nil
   :active-frame nil
   :socket-connection nil
   :games []
   :errors []})
