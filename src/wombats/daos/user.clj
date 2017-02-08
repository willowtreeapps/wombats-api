(ns wombats.daos.user
  (:require [datomic.api :as d]
            [wombats.daos.helpers :refer [gen-id
                                          db-requirement-error
                                          get-entity-id
                                          get-entity-by-prop
                                          get-entities-by-prop
                                          retract-entity-by-prop]]))

(def public-user-fields [:db/id
                         :user/id
                         :user/github-username
                         :user/avatar-url])

(defn get-user-entity-id
  "Returns the entity id of a user given the public user id"
  [conn user-id]
  (get-entity-id conn :user/id user-id))

(defn get-users
  "Returns all users in the system"
  [conn]
  (fn []
    (get-entities-by-prop conn :user/id public-user-fields)))

(defn get-user-by-id
  "Returns a user by a given id"
  [conn]
  (fn [user-id]
    (get-entity-by-prop conn :user/id user-id public-user-fields)))

(defn get-user-by-email
  "Returns a user by a given email"
  [conn]
  (fn [user-email]
    (get-entity-by-prop conn :user/email user-email public-user-fields)))

(defn get-user-by-access-token
  "Returns a user by a given access token"
  [conn]
  (fn [access-token]
    (get-entity-by-prop conn :user/access-token access-token)))

(defn get-user-by-github-id
  "Returns a user by a given github-id"
  [conn]
  (fn [github-id]
    (get-entity-by-prop conn :user/github-id github-id)))

(defn create-or-update-user
  "If a user does not exist in the system, create one. If it does, update values
  and attach the new access token"
  [conn]
  (fn [{:keys [login id avatar_url] :as user}
      github-access-token
      user-access-token
      current-user-id]
    (let [update {:user/github-access-token github-access-token
                  :user/access-token user-access-token
                  :user/github-username login
                  :user/github-id id
                  :user/avatar-url avatar_url}]
      (if-not current-user-id
        (d/transact-async conn [(merge update {:db/id (d/tempid :db.part/user)
                                               :user/id (gen-id)})])
        (d/transact-async conn [(merge update {:user/id current-user-id})])))))

(defn remove-access-token
  [conn]
  (fn [access-token]
    (let [user ((get-user-by-access-token conn) access-token)]
      (if user
        (d/transact-async conn [[:db/retract (:db/id user)
                                 :user/access-token access-token]])
        (future)))))

(defn get-user-wombats
  "Returns all wombats belonging to a specified user"
  [conn]
  (fn [user-id]
    (vec (apply concat
                (d/q '[:find (pull ?wombats [:wombat/name
                                             :wombat/url
                                             :wombat/id])
                       :in $ ?user-id
                       :where [?user :user/id ?user-id]
                              [?user :user/wombats ?wombat]
                              [?wombats :wombat/owner ?user]]
                     (d/db conn)
                     user-id)))))

(defn get-wombat-by-name
  "Returns a wombat by querying its name"
  [conn]
  (fn [name]
    (get-entity-by-prop conn :wombat/name name)))

(defn get-wombat-by-id
  "Returns a wombat by querying its id"
  [conn]
  (fn [wombat-id]
    (get-entity-by-prop conn :wombat/id wombat-id)))

(defn get-wombat-by-url
  "Returns a wombat by querying its url"
  [conn]
  (fn [wombat-url]
    (get-entity-by-prop conn :wombat/url wombat-url)))

(defn get-wombat-owner-id
  [conn]
  (fn [wombat-id]
    (ffirst
     (d/q '[:find ?user-id
            :in $ ?wombat-id
            :where [?wombat :wombat/id ?wombat-id]
                   [?user :user/wombats ?wombat]
                   [?user :user/id ?user-id]]
          (d/db conn)
          wombat-id))))

(defn- ensure-wombat-name-availability
  "Ensure that a wombat does not exist with the given name"
  [conn wombat-name]
  (let [wombat ((get-wombat-by-name conn) wombat-name)]
    (when wombat
      (db-requirement-error
       (str "Wombat with name '" wombat-name "' is already in use")))))

(defn- ensure-wombat-url-availability
  "Ensure that a wombat does not exist with the given name"
  [conn wombat-url]
  (let [wombat ((get-wombat-by-url conn) wombat-url)]
    (when wombat
      (db-requirement-error
       (str "Wombat source code was already registered. If you own the source code, change the file name and try again.")))))

(defn add-user-wombat
  "Creates a new wombat for a particular user"
  [conn]
  (fn [user-eid
      {:keys [:wombat/name
              :wombat/url
              :wombat/id]}]
    (let [wombat-eid (d/tempid :db.part/user)]

      (ensure-wombat-name-availability conn name)
      (ensure-wombat-url-availability conn url)

      (d/transact conn [{:db/id wombat-eid
                         :wombat/owner user-eid
                         :wombat/name name
                         :wombat/url url
                         :wombat/id id}
                        {:db/id user-eid
                         :user/wombats wombat-eid}]))))

(defn update-user-wombat
  "Update a wombat"
  [conn]
  (fn [{:keys [:wombat/name
              :wombat/url
              :wombat/id]}]
    (let [{eid :db/id
           prev-name :wombat/name
           prev-url :wombat/url} ((get-wombat-by-id conn) id)]

      (when-not (= prev-name name)
        (ensure-wombat-name-availability conn name))

      (when-not (= prev-url url)
        (ensure-wombat-url-availability conn url))

      (d/transact conn [{:db/id eid
                         :wombat/name name
                         :wombat/url url}]))))

(defn retract-wombat
  "Retracts a wombat"
  [conn]
  (fn [wombat-id]
    (retract-entity-by-prop conn :wombat/id wombat-id)))
