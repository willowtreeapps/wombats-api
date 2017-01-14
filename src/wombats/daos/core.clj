(ns wombats.daos.core
  (:require [wombats.daos.user :as user]))

(defn init-dao-map
  "Creates a map of all the data accessors that can be used inside of handlers / socket connections. This makes no assumption of authentication / authorization which should be handled prior to gaining access to these functions."
  [{:keys [conn] :as datomic}]
  {:get-users (user/get-users conn)
   :add-user (user/add-user conn)})

(defn get-fn
  [fn-key context]
  (fn-key (or (:wombats.interceptors.dao/daos context) {})))
