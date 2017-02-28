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
                                               :code :player-join-db-fn})))

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
                                               :code :player-join-db-fn})))


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
                                               :code :player-join-db-fn})))

             ;; This next part builds up the transaction(s)
             ;; 1. Creates the player trx
             ;; 2. Adds player to the game
             ;; 3. Adds a stat entity to the game that belongs to the new player
             ;; 4. If the game is now full, add the :pending closed transaction
             (let [player-tmpid (d/tempid :db.part/user)
                   stats-tmpid (d/tempid :db.part/user)
                   player-trx {:db/id player-tmpid
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

(defn seed-database-functions
  [conn]
  (d/transact conn [{:db/id (d/tempid :db.part/user)
                     :db/ident :player-join
                     :db/doc "Transaction for players joining a game."
                     :db/fn player-join}
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
