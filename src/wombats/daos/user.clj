(ns wombats.daos.user
  (:require [datomic.api :as d]))

(defn get-users
  [conn]
  (fn []
    (vec (d/q '[:find ?e
                :where [?e :user/username]]
              (d/db conn)))))

(defn add-user
  [conn]
  (fn [{:keys [username]}]
    (d/transact-async conn [{:db/id (d/tempid :db.part/user)
                             :user/username username}])))
