(ns battlebots.services.mongodb
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]
            [monger.operators :refer :all])
  (:import org.bson.types.ObjectId))

(def is-production? (= (System/getenv "CLJ_ENV") "production"))
(def db-username (System/getenv "WT_BATTLEBOTS_MONGOD_USER_NAME"))
(def db-password (System/getenv "WT_BATTLEBOTS_MONGOD_USER_PW"))
(def db-host (System/getenv "WT_BATTLEBOTS_MONGOD_HOST_LIST"))

;; Throw Exception if running in Prod with no database environment variables
(if is-production?
  (if (not (and db-username db-password db-host))
    (throw (Exception. "Missing Database Environment Variable"))))

(def connection-uri (if is-production?
                      (str "mongodb://" db-username ":" db-password "@" db-host "/battlebots")
                      "mongodb://127.0.0.1/battlebots"))

(defn setup-db
  "Ensures collection indexes exist"
  [db]
  (let [players "players"]

    ;; Player indexes
    (mc/ensure-index db players (array-map
                                 :github-id 1
                                 :access-token 1) {:unique true})))

(def conn
  (let [conn (atom (mg/connect-via-uri connection-uri))
        db (:db @conn)]
    (setup-db db)
    conn))

(defn get-db [] (:db @conn))

(def games-coll "games")
(def rounds-coll "rounds")
(def game-fields [:initial-arena
                  :players
                  :state])

;; GAME OPERATIONS
(defn get-all-games
  []
  (with-collection (get-db) games-coll
    (find {})
    (fields game-fields)))

(defn get-game
  [game-id]
  (first (with-collection (get-db) games-coll
             (find {:_id (ObjectId. game-id)})
             (fields game-fields)
             (limit 1))))

(defn add-game
  [game]
  (mc/insert-and-return (get-db) games-coll game))

(defn update-game
  [game-id update]
  (mc/update-by-id (get-db) games-coll (ObjectId. game-id) update))

(defn add-player-to-game
  [game-id player]
  (mc/update (get-db) games-coll {:_id (ObjectId. game-id)} {$push {:players player}}))

(defn remove-game
  [game-id]
  (mc/remove (get-db) rounds-coll {:game-id (ObjectId. game-id)})
  (mc/remove-by-id (get-db) games-coll (ObjectId. game-id)))

(defn save-game-segment
  [game-segment]
  (mc/insert (get-db) rounds-coll game-segment))

(defn get-game-segment-count
  [game-id]
  (mc/count (get-db) rounds-coll {:game-id (ObjectId. game-id)}))

(defn get-game-segment
  [game-id segment-number]
  (mc/find-one-as-map (get-db) rounds-coll {:game-id (ObjectId. game-id)
                                            :segment segment-number}))
;; PLAYER OPERATIONS

(def player-coll "players")
(def player-fields [:admin
                    :avatar_url
                    :email
                    :login
                    :github-id
                    :name
                    :bots])

(defn get-all-players
  []
  (with-collection (get-db) player-coll
    (find {})
    (fields player-fields)))

(defn get-player
  [player-id]
  (first (with-collection (get-db) player-coll
           (find {:_id (ObjectId. player-id)})
           (limit 1)
           (fields player-fields))))

(defn get-player-with-token
  [player-id]
  (first (with-collection (get-db) player-coll
           (find {:_id (ObjectId. player-id)})
           (limit 1)
           (fields (conj player-fields :access-token)))))

(defn add-or-update-player
  [player]
  (mc/save-and-return (get-db) player-coll player))

(defn get-player-by-github-id
  [github-id]
  (first (with-collection (get-db) player-coll
           (find {:github-id github-id})
           (limit 1)
           (fields player-fields))))

(defn get-player-by-auth-token
  "NOTE: get-player-by-auth-token is used by the middleware layer and exposes
  the entire player map"
  [access-token]
  (mc/find-one-as-map (get-db) player-coll {:access-token access-token}))

(defn remove-player
  [player-id]
  (mc/remove-by-id (get-db) player-coll (ObjectId. player-id)))

(defn add-player-bot
  [player-id bot]
  (mc/update (get-db) player-coll {:_id (ObjectId. player-id)} {$push {:bots bot}}))

(defn remove-player-bot
  [player-id repo]
  (mc/update (get-db) player-coll {:_id (ObjectId. player-id)} {$pull {:bots {:repo repo}}}))
