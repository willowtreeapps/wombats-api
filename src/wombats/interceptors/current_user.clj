(ns wombats.interceptors.current-user
  (:require [wombats.daos.helpers :refer [get-fn]]))

(def add-current-user
  "Attaches the current user to the context map"
  {:name ::current-user-interceptor
   :enter (fn [{:keys [request] :as context}]
            (let [access-token (get-in request [:headers "authorization"])
                  get-user (get-fn :get-user-by-access-token context)
                  user (when access-token (get-user access-token))]
              (assoc context ::current-user user)))})

(defn get-current-user
  [context]
  (::current-user context))
