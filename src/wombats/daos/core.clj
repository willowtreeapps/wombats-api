(ns wombats.daos.core
  (:require [wombats.daos.user :as user]))

(defn init-dao-map
  "Creates a map of all the data accessors that can be used inside of handlers / socket connections.
  This makes no assumption of authentication / authorization which should be handled prior to gaining
  access to these functions."
  [{:keys [conn] :as datomic}]
  {:get-users (user/get-users conn)
   :get-user-by-id (user/get-user-by-id conn)
   :get-user-by-email (user/get-user-by-email conn)
   :get-user-by-access-token (user/get-user-by-access-token conn)
   :create-or-update-user (user/create-or-update-user conn)})

(defn get-fn
  "Helper function used to pull the correct dao out of context"
  [fn-key context]
  (let [dao-fn (fn-key (:wombats.interceptors.dao/daos context))]
    (if dao-fn
      dao-fn
      (throw (Exception. (str "Dao function " fn-key " was not found in the dao map"))))))
