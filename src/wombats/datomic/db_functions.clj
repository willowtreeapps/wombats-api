(ns wombats.datomic.db-functions
  (:require [datomic.api :as d]))

(def player-join
  (d/function
   '{:lang :clojure
     :params [db game-eid user-eid wombat-eid color initial-stats]
     :code (let [{game-id :game/id
                  status :game/status
                  current-players :game/players
                  max-allotted-players :game/max-players} (datomic.api/entity db game-eid)
                 {wombat-id :wombat/id} (datomic.api/entity db wombat-eid)
                 {user-id :user/id} (datomic.api/entity db user-eid)]

             (when-not game-id
               (throw (ex-info "Wombat Error" {:type :wombat-error
                                               :message "Game cound not be found"
                                               :details {:game-eid game-eid}
                                               :code :player-join-db-fn})))

             (when (>= (count current-players) max-allotted-players)
               (throw (ex-info "Wombat Error" {:type :wombat-error
                                               :message "Game full."
                                               :details {:game-eid game-eid}
                                               :code :game-full})))

             (when-not wombat-id
               (throw (ex-info "Wombat Error" {:type :wombat-error
                                               :message "Wombat could not be found"
                                               :details {:game-eid game-eid
                                                         :wombat-eid wombat-eid}
                                               :code :player-join-db-fn})))

             (when-not user-id
               (throw (ex-info "Wombat Error" {:type :wombat-error
                                               :message "User could not be found"
                                               :details {:game-eid game-eid
                                                         :user-eid user-eid}
                                               :code :player-join-db-fn})))

             (when-not (= status :pending-open)
               (throw (ex-info "Wombat Error" {:type :wombat-error
                                               :message (d/invoke db :get-closed-enrollment-error-message status)
                                               :details {:game-eid game-eid
                                                         :status status}
                                               :code (keyword (str "unable-to-join-" (name status)))})))


             (when (d/invoke db :player-in-game? db game-id user-eid)
               (throw (ex-info "Wombat Error" {:type :wombat-error
                                               :message "Already in game."
                                               :details {:game-eid game-eid
                                                         :user-eid user-eid}
                                               :code :player-join-db-fn})))

             (when (d/invoke db :color-taken? db game-id color)
               (throw (ex-info "Wombat Error" {:type :wombat-error
                                               :message "Color taken."
                                               :details {:game-eid game-eid
                                                         :color color}
                                               :code :player-join-db-fn
                                               :field-error :wombat-color})))

             ;; This next part builds up the transaction(s)
             ;; 1. Creates the player trx
             ;; 2. Adds player to the game
             ;; 3. Adds a stat entity to the game that belongs to the new player
             ;; 4. If the game is now full, add the :pending closed transaction
             (let [player-tmpid (d/tempid :db.part/user)
                   stats-tmpid (d/tempid :db.part/user)
                   player-trx {:db/id player-tmpid
                               :player/id (str (java.util.UUID/randomUUID))
                               :player/user user-eid
                               :player/wombat wombat-eid
                               :player/color color}
                   join-trx {:db/id game-eid
                             :game/players player-tmpid}
                   stats-trx (merge {:db/id stats-tmpid
                                     :stats/player player-tmpid
                                     :stats/game game-eid}
                                    initial-stats)
                   stats-link-to-game-trx {:db/id game-eid
                                           :game/stats stats-tmpid}
                   closed-trx {:db/id game-eid
                               :game/status :pending-closed}]
               (cond-> []
                 true (conj player-trx)
                 true (conj join-trx)
                 true (conj stats-trx)
                 true (conj stats-link-to-game-trx)
                 (= (+ (count current-players) 1) max-allotted-players) (conj closed-trx))))}))

(def get-closed-enrollment-error-message
  (d/function
   '{:lang :clojure
     :params [status]
     :code (case status
             :active "This game has already been started."
             :active-intermission "This game has already been started."
             :pending-closed "This game is no longer accepting new players."
             :closed "The game you are trying to join is over."
             "Something went wrong while attempting to join this game.")}))

(def player-in-game?
  (d/function
   '{:lang :clojure
     :params [db game-id user-eid]
     :code (let [player
                 (d/q '[:find ?player
                        :in $ ?game-id ?user-eid
                        :where
                        [?game :game/id ?game-id]
                        [?game :game/players ?player]
                        [?player :player/user ?user-eid]]
                      db
                      game-id
                      user-eid)]
             (> (count player) 0))}))

(def color-taken?
  (d/function
   '{:lang :clojure
     :params [db game-id color]
     :code (let [player
                 (d/q '[:find ?player
                        :in $ ?game-id ?color
                        :where
                        [?game :game/id ?game-id]
                        [?game :game/players ?player]
                        [?player :player/color ?color]]
                      db
                      game-id
                      color)]
             (> (count player) 0))}))

(def add-or-update-access-key
  (d/function
   '{:lang :clojure
     :params [db access-key]
     :code (let [{requesting-key :access-key/key
                  requesting-eid :db/id} access-key

                 current-access-key
                 (ffirst
                  (d/q '[:find (pull ?access-key [*])
                         :in $ ?access-key-key
                         :where
                         [?access-key :access-key/key ?access-key-key]]
                       db
                       requesting-key))]

             (when (and current-access-key
                        (not= (:db/id current-access-key)
                              requesting-eid))
               (throw (ex-info "Wombat Error" {:type :wombat-error
                                               :message "Access key already in use."
                                               :details {:access-key/key requesting-key
                                                         :db/id requesting-eid}
                                               :code :transactor/access-key-in-use})))

             [(if (some? requesting-eid)
                access-key
                (assoc access-key :db/id (d/tempid :db.part/user)))])}))

(def create-or-update-user
  (d/function
   '{:lang :clojure
     :params [db github-user github-access-token user-access-token access-key-key]
     :code (let [{github-id :id
                  github-username :login
                  avatar-url :avatar_url} github-user
                 access-key (when access-key-key
                                 (ffirst
                                  (d/q '[:find (pull ?access-key [*])
                                         :in $ ?access-key-key
                                         :where [?access-key :access-key/key ?access-key-key]]
                                       db access-key-key)))
                 valid-access-key? (and
                                    ;; Access key exists
                                    access-key
                                    ;; Access key has not reached max uses
                                    (< (:access-key/number-of-uses access-key)
                                       (:access-key/max-number-of-uses access-key))
                                    ;; Access key has not expired
                                    (< (.getTime (new java.util.Date))
                                       (.getTime (:access-key/expiration-date access-key))))
                 current-user (ffirst
                               (d/q '[:find ?user
                                      :in $ ?github-id
                                      :where [?user :user/github-id ?github-id]]
                                    db github-id))
                 current-user-id (get current-user :user/id)
                 user-update (cond-> {:user/github-access-token github-access-token
                                      :user/access-token user-access-token
                                      :user/github-username github-username
                                      :user/github-id github-id
                                      :user/avatar-url avatar-url}
                               (and access-key
                                    (nil? (:user/access-key current-user)))
                               (assoc :user/access-key (:db/id access-key)))]
             (cond-> []
               (nil? current-user-id)
               (conj (merge user-update
                            {:db/id (d/tempid :db.part/user)
                             :user/id (.toString (java.util.UUID/randomUUID))
                             :user/roles [:user.roles/user]}))

               (some? current-user-id)
               (conj (merge user-update {:user/id current-user-id}))

               valid-access-key?
               (conj {:access-key/key (:access-key/key access-key)
                      :access-key/number-of-uses (inc (:access-key/number-of-uses access-key))})))}))

(defn seed-database-functions
  [conn]
  (d/transact conn [{:db/id (d/tempid :db.part/user)
                     :db/ident :player-join
                     :db/doc "Transaction for players joining a game."
                     :db/fn player-join}
                    {:db/id (d/tempid :db.part/user)
                     :db/ident :add-or-update-access-key
                     :db/doc "Transaction for adding or updating access keys"
                     :db/fn add-or-update-access-key}
                    {:db/id (d/tempid :db.part/user)
                     :db/ident :create-or-update-user
                     :db/doc "Transaction for creating or updating a users credentials"
                     :db/fn create-or-update-user}
                    {:db/id (d/tempid :db.part/user)
                     :db/ident :get-closed-enrollment-error-message
                     :db/doc "Returns the error message when a game is considered in a closed enrollment state."
                     :db/fn get-closed-enrollment-error-message}
                    {:db/id (d/tempid :db.part/user)
                     :db/ident :player-in-game?
                     :db/doc "Database function that checks if a player exists in a game."
                     :db/fn player-in-game?}
                    {:db/id (d/tempid :db.part/user)
                     :db/ident :color-taken?
                     :db/doc "Database function that checks if a color has already been chosen in a game."
                     :db/fn color-taken?}]))
