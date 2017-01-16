(ns wombats.daos.user
  (:require [datomic.api :as d]))

;; TODO: This doesn't work when imported into the query
(def public-user-fields [:user/id
                         :user/github-username
                         :user/avatar-url])

(defn get-users
  [conn]
  (fn []
    (apply concat
           (d/q '[:find (pull ?user [:user/email
                                     :user/github-username
                                     :user/avatar-url])
                  :in $
                  :where
                  [?user :user/id]]
                (d/db conn)))))

(defn get-user-by-id
  "Gets a user by a given id"
  [conn]
  (fn [id]
    (d/pull (d/db conn) '[*] [:user/id id])))

(defn get-user-by-email
  "Gets a user by a given email"
  [conn]
  (fn [email]
    (d/pull (d/db conn) '[*] [:user/email email])))

(defn get-user-by-access-token
  "Gets a user by a given access token"
  [conn]
  (fn [access-token]
    (d/pull (d/db conn) '[*] [:user/access-token access-token])))

(defn create-or-update-user
  "If a user does not exist in the system, create one. If it does, update values
  and attach the new access token"
  [conn]
  (fn [{:keys [email login id avatar_url] :as user}
      access-token
      current-user-id]
    (let [update {:db/id (d/tempid :db.part/user)
                  :user/access-token access-token
                  :user/github-username login
                  :user/github-id id
                  :user/email email
                  :user/avatar-url avatar_url}]
      (if current-user-id
        (d/transact-async conn [(merge update {:db/id current-user-id
                                               :user/id (str (java.util.UUID/randomUUID))})])
        (d/transact-async conn [update])))))
