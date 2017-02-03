(ns wombats.daos.game
  (:require [datomic.api :as d]
            [taoensso.nippy :as nippy]
            [wombats.game.core :refer [initialize-game]]
            [wombats.game.utils :refer [decision-maker-state]]
            [wombats.daos.helpers :refer [db-requirement-error
                                          get-entity-by-prop
                                          get-entity-id
                                          gen-id
                                          get-entities-by-prop
                                          retract-entity-by-prop]]
            [wombats.daos.user :refer [get-user-entity-id
                                       public-user-fields]]))

;; Note about game states:
;;
;; There are four different states that a game can be in.
;; 1. :pending-open   (the game is awaiting players to join and the game has not yet begun)
;; 2. :pending-closed (the game is now longer accepting new player enrollment and the game has not yet begun)
;; 3. :active         (the game is underway)
;; 4. :closed         (the game is finished and archived)
;;
;; Right now these states are hardcoded in their respective DAOs, but this logic should be captured in a FSM
;; like https://github.com/ztellman/automat or even a simple hand rolled one.
;;
;; Looks something like this;
;;
;; 1 → 3 → 4
;; ↓   ↑
;; 2 → ↑

(defn get-all-games
  [conn]
  (fn []
    (get-entities-by-prop conn :game/id)))

(defn get-game-eids-by-status
  [conn]
  (fn [status]
    (let [db (d/db conn)
          formatted-status (if (vector? status)
                             (map keyword status)
                             [(keyword status)])
          game-eids (apply concat
                           (d/q '[:find ?games
                                  :in $ [?status ...]
                                  :where [?games :game/status ?status]]
                                db
                                formatted-status))]
      game-eids)))

(defn get-game-eids-by-player
  [conn]
  (fn [user-id]
    (let [user-ids (if (vector? user-id)
                     user-id
                     [user-id])
          game-eids (apply concat
                           (d/q '[:find ?games
                                  :in $ [?user-ids ...]
                                  :where [?users :user/id ?user-ids]
                                         [?players :player/user ?users]
                                         [?games :game/players ?players]]
                                (d/db conn)
                                user-ids))]
      game-eids)))

(defn get-games-by-eids
  [conn]
  (fn [game-eids]
    (d/pull-many (d/db conn) '[*] game-eids)))

(defn get-game-by-id
  [conn]
  (fn [game-id]
    (get-entity-by-prop conn :game/id game-id)))

(defn add-game
  "Adds a new game entity to Datomic"
  [conn]
  (fn [{:keys [:game/id
              :game/name
              :game/max-players
              :game/type
              :game/is-private
              :game/password
              :game/num-rounds
              :game/start-time
              :game/round-intermission]} arena-eid game-arena]

    (let [frame-tmp-id (d/tempid :db.part/user)
          frame-trx {:db/id frame-tmp-id
                     :frame/frame-number 0
                     :frame/id (gen-id)
                     :frame/arena (nippy/freeze game-arena)}
          game-trx {:db/id (d/tempid :db.part/user)
                    :game/id id
                    :game/frame frame-tmp-id
                    :game/name name
                    :game/max-players max-players
                    :game/type type
                    :game/is-private is-private
                    :game/password (or password "")
                    :game/num-rounds (or num-rounds 1)
                    :game/start-time (read-string (str "#inst \"" start-time "\""))
                    :game/round-intermission (or round-intermission 0)
                    :game/arena arena-eid
                    :game/status :pending-open}]
      (d/transact-async conn [frame-trx
                              game-trx]))))

(defn retract-game
  [conn]
  (fn [game-id]
    (retract-entity-by-prop conn :game/id game-id)))

(defn- game-full?
  "Check if the game is full by comparing the currnent number of players to the
  max-players attribute"
  ([game]
   (game-full? game 0))
  ([{:keys [:game/players :game/max-players]} add-n-players]
   (= (+ (count (or players []))
         add-n-players)
      max-players)))

(defn- player-in-game?
  "Check to see if a player is already in a game

  TODO: Figure out how to query the game id in the datomic query"
  [conn user-eid game-eid]
  (let [games (set
               (apply concat
                      (d/q '[:find ?games
                             :in $ ?user-eid
                             :where [?players :player/user ?user-eid]
                                    [?games :game/players ?players]]
                           (d/db conn)
                           user-eid)))]
    (contains? games game-eid)))

(defn- color-taken?
  [conn game-id color]
  (let [player (ffirst
                (d/q '[:find ?player
                       :in $ ?game-id ?color
                       :where [?game :game/id ?game-id]
                              [?game :game/players ?player]
                              [?player :player/color ?color]]
                     (d/db conn)
                     game-id
                     color))]
    (boolean player)))

(defn- open-for-enrollment?
  "Check if game is open for enrollment"
  [{:keys [:game/status]}]
  (= status :pending-open))

(defn add-player-to-game
  [conn]
  (fn [game-id user-eid wombat-eid color]
    (let [game ((get-game-by-id conn) game-id)
          game-eid (:db/id game)]

      ;; Check for game existence
      (when-not game
        (db-requirement-error
         (str "Game '" game-eid "' was not found")))

      ;; Check to see if the game is accepting new players
      (when-not (open-for-enrollment? game)
        (db-requirement-error
         (str "Game '" game-eid "' is not accepting new players")))

      ;; Check to see if the player is already in the game
      (when (player-in-game? conn user-eid game-eid)
        (db-requirement-error
         (str "User '" user-eid "' is already in game '" game-eid "'")))

      ;; Check for available color
      (when (color-taken? conn game-id color)
        (db-requirement-error
         (str "Color '" color "' is already in use")))

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
            stats-trx {:db/id stats-tmpid
                       :stats/player player-tmpid
                       :stats/game game-eid
                       :stats/frame-number 0
                       :stats/score 0
                       :stats/wombats-destroyed 0
                       :stats/wombats-hit 0
                       :stats/zakano-destroyed 0
                       :stats/zakano-hit 0
                       :stats/wood-barriers-destroyed 0
                       :stats/wood-barriers-hit 0
                       :stats/shots-fired 0
                       :stats/shots-hit 0
                       :stats/number-of-moves 0
                       :stats/number-of-smoke-deploys 0}
            stats-link-to-game-trx {:db/id game-eid
                                    :game/stats stats-tmpid}
            closed-trx {:db/id game-eid
                        :game/status :pending-closed}
            trx (cond-> []
                  true (conj player-trx)
                  true (conj join-trx)
                  true (conj stats-trx)
                  true (conj stats-link-to-game-trx)
                  (game-full? game 1) (conj closed-trx))]

        (d/transact-async conn trx)))))

(defn- format-player-map
  "Formats the player map attached to game-state"
  [players]
  (let [formatted-players (map (fn [[player stats user wombat]]
                                 {:player player
                                  :stats stats
                                  :user user
                                  :wombat wombat
                                  :state decision-maker-state}) players)]
    (reduce #(assoc %1 (gen-id) %2) {} formatted-players)))

(defn- get-game-state
  [conn game-id]
  (let [[frame
         arena] (first
                 (d/q '[:find (pull ?frame [*])
                              (pull ?arena [*])
                        :in $ ?game-id
                        :where [?game :game/id ?game-id]
                               [?game :game/frame ?frame]
                               [?game :game/arena ?arena]]
                      (d/db conn)
                      game-id))
        players (d/q '[:find (pull ?players [*])
                             (pull ?stats [*])
                             (pull ?user [:db/id
                                          :user/github-username
                                          :user/access-token])
                             (pull ?wombat [*])
                       :in $ ?game-id
                       :where [?game :game/id ?game-id]
                              [?game :game/players ?players]
                              [?game :game/stats ?stats]
                              [?players :player/user ?user]
                              [?players :player/wombat ?wombat]]
                     (d/db conn)
                     game-id)]

    {:game-id game-id
     :frame (update frame :frame/arena nippy/thaw)
     :arena-config arena
     :players (format-player-map players)}))

(defn start-game
  "Transitions the game status to active"
  [conn aws-credentials]
  (fn [game]
    (let [{game-id :game/id
           game-eid :db/id} game
          game-state (get-game-state conn game-id)]

      (initialize-game game-state aws-credentials)

      (db-requirement-error
       (str "Color '" "' is already in use"))

      (d/transact-async conn [{:db/id game-eid
                               :game/status :active}]))))
