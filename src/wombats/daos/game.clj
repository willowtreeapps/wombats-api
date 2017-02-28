(ns wombats.daos.game
  (:require [datomic.api :as d]
            [taoensso.nippy :as nippy]
            [wombats.constants :refer [initial-stats]]
            [wombats.game.core :as game]
            [wombats.game.utils :refer [decision-maker-state]]
            [wombats.sockets.game :as game-sockets]
            [wombats.daos.helpers :refer [get-entity-by-prop
                                          get-entity-id
                                          gen-id
                                          get-entities-by-prop
                                          retract-entity-by-prop]]
            [wombats.handlers.helpers :refer [wombat-error]]
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

(def game-projection
  '[*
    {:game/players [*
                    {:player/user [:user/github-username]}
                    {:player/wombat [*]}]}
    {:game/stats [*]}])

(defn get-games-by-eids
  [conn]
  (fn [game-eids]
    (d/pull-many
     (d/db conn) '[*
                   {:game/arena [:db/id *]}
                   {:game/players [:db/id :player/color
                                   {:player/user [:db/id :user/github-username]}
                                   {:player/wombat [:db/id :wombat/name]}]}]
     game-eids)))

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

(defn get-all-game-eids
  [conn]
  (fn []
    (apply concat
           (d/q '[:find ?games
                  :in $
                  :where [?games :game/id]]
                (d/db conn)))))

(defn get-all-games
  [conn]
  (fn []
    ((get-games-by-eids conn)
     ((get-all-game-eids conn)))))

(defn get-all-pending-games
  [conn]
  (fn []
    ((get-games-by-eids conn)
     ((get-game-eids-by-status conn) [:pending-open :pending-closed]))))

(defn get-game-by-id
  [conn]
  (fn [game-id]
    (get-entity-by-prop conn :game/id game-id game-projection)))

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

(defn get-game-state-by-id
  [conn]
  (fn [game-id]
    (let [[frame
           arena
           game] (first
                   (d/q '[:find (pull ?frame [*])
                                (pull ?arena [*])
                                (pull ?game [*])
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
                                            :user/github-access-token])
                               (pull ?wombat [*])
                         :in $ ?game-id
                         :where [?game :game/id ?game-id]
                                [?game :game/players ?players]
                                [?game :game/stats ?stats]
                                [?players :player/user ?user]
                                [?players :player/wombat ?wombat]]
                       (d/db conn)
                       game-id)]

      ;; TODO The datomic query pulls 2 of each player. The following will filter
      ;;      out the duplicates.
      (let [n-players (vec
                       (vals
                        (reduce (fn [player-acc player]
                                  (let [id (:db/id (first player))
                                        existing-ids (set (vals player-acc))]
                                    (if (contains? existing-ids id)
                                      player-acc
                                      (assoc player-acc id player))))
                                {} players)))]

        {:game-id game-id
         :frame (update frame :frame/arena nippy/thaw)
         :arena-config arena
         :game-config game
         :players (format-player-map n-players)}))))

(defn add-game
  "Adds a new game entity to Datomic"
  [conn]
  (fn [game arena-eid game-arena]

    (let [frame-tmp-id (d/tempid :db.part/user)
          frame-trx {:db/id frame-tmp-id
                     :frame/frame-number 0
                     :frame/round-number 1
                     :frame/id (gen-id)
                     :frame/arena (nippy/freeze game-arena)}
          game-trx (merge game
                          {:db/id (d/tempid :db.part/user)
                           :game/frame frame-tmp-id
                           :game/arena arena-eid})]
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

(defn- get-closed-enrollment-error-code
  [{:keys [:game/status]}]
  (case status
    :active 101007
    :active-intermission 101007
    :pending-closed 101001
    :closed 101008
    101009))

(defn add-player-to-game
  [conn]
  (fn [game user-eid wombat-eid color]
    (let [{game-id :game/id
           game-eid :db/id} game]

      ;; Check to see if the game is accepting new players
      (when-not (open-for-enrollment? game)
        (wombat-error {:code (get-closed-enrollment-error-code game)}))

      ;; Check to see if the player is already in the game
      (when (player-in-game? conn user-eid game-eid)
        (wombat-error {:code 101002
                       :details {:user-eid user-eid
                                 :game-eid game-eid}}))

      ;; Check for available color
      (when (color-taken? conn game-id color)
        (wombat-error {:code 101003
                       :params [color]}))

      (when-not user-eid
        (wombat-error {:code 101004
                       :details {:user-eid user-eid}}))

      (when-not wombat-eid
        (wombat-error {:code 101005
                       :details {:wombat-eid wombat-eid}}))

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
                        :game/status :pending-closed}
            trx (cond-> []
                  true (conj player-trx)
                  true (conj join-trx)
                  true (conj stats-trx)
                  true (conj stats-link-to-game-trx)
                  (game-full? game 1) (conj closed-trx))]

        (future
          (d/transact conn trx)

          (let [game-state ((get-game-state-by-id conn) game-id)]
            (game-sockets/broadcast-game-info game-state)))))))

(defn- update-frame-state
  [conn]
  (fn [frame
      players]

    (let [frame-trx (-> frame (update :frame/arena nippy/freeze))
          stats-trxs (vec (map (fn [[_ {stats :stats}]]
                                 (assoc stats
                                        :stats/frame-number
                                        (:frame/frame-number frame)))
                               players))]
      (d/transact-async conn (conj stats-trxs frame-trx)))))

(defn- close-round
  [conn]
  (fn [{:keys [frame game-config]}]

    (let [frame-trx (-> frame (update :frame/arena nippy/freeze))
          game-trx game-config]

      (d/transact-async conn [frame-trx game-trx]))))

(defn- close-game-state
  [conn]
  (fn [{:keys [game-id game-config]}]

    (d/transact-async conn [{:game/id game-id
                             :game/status :closed
                             :game/end-time (:game/end-time game-config)}])))

(defn start-game
  "Transitions the game status to active"
  [conn aws-credentials]
  (fn [game-id]
    (let [game-state ((get-game-state-by-id conn) game-id)
          {game-eid :db/id} game-state]

      (when (= 0 (count (:players game-state)))
        (wombat-error {:code 101006
                       :details {:game-id game-id}}))

      ;; We put this in a future so that it gets run on a separate thread
      (future
        (game/start-round game-state
                          {:update-frame (update-frame-state conn)
                           :close-round (close-round conn)
                           :close-game (close-game-state conn)
                           :round-start-fn (start-game conn aws-credentials)}
                          aws-credentials))

      (d/transact-async conn [{:game/id game-id
                               :game/status :active}]))))

(defn get-player-from-game
  "Returns the player entity from a specified game"
  [conn]
  (fn [game-id user-id]
    (let [player (ffirst
                  (d/q '[:find (pull ?player [*])
                         :in $ ?game-id ?user-id
                         :where [?user :user/id ?user-id]
                                [?player :player/user ?user]
                                [?game :game/players ?player]]
                       (d/db conn)
                       game-id
                       user-id))]
      player)))
