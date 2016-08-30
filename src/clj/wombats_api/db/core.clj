(ns wombats-api.db.core
    (:require [monger.core :as mg]
              [monger.collection :as mc]
              [monger.query :as q]
              [monger.operators :refer :all]
              [mount.core :refer [defstate]]
              [wombats-api.config :refer [env]])
    (:import org.bson.types.ObjectId)
    (:import com.mongodb.WriteConcern))

(defstate db*
  :start (-> env :database-url mg/connect-via-uri)
  :stop (-> db* :conn mg/disconnect))

(defstate db
  :start (:db db*))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; GAME OPERATIONS
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^{:private true} games-coll "games")
(def ^{:private true} frames-coll "frames")

(defn get-all-games
  []
  (q/with-collection db games-coll
    (q/find {})))

(defn get-game
  [game-id]
  (first (q/with-collection db games-coll
             (q/find {:_id (ObjectId. game-id)})
             (q/limit 1))))

(defn add-game
  [game]
  (mc/insert-and-return db games-coll game))

(defn update-game
  [game-id update]
  (mc/update-by-id db games-coll (ObjectId. game-id) update))

(defn add-player-to-game
  [game-id player]
  (mc/update db games-coll {:_id (ObjectId. game-id)} {$push {:players player}}))

(defn remove-game
  [game-id]
  (mc/remove db frames-coll {:game-id (ObjectId. game-id)})
  (mc/remove-by-id db games-coll (ObjectId. game-id)))

(defn save-game-round
  [frames]
  (mc/insert-batch db frames-coll frames WriteConcern/NORMAL))

(defn get-game-frames
  ([game-id]
   (q/with-collection db frames-coll
     (q/find {:game-id (ObjectId. game-id)})))
  ([game-id frame-id]
   (first (q/with-collection db frames-coll
            (q/find {:game-id (ObjectId. game-id)
                     :_id (ObjectId. frame-id)})
            (q/limit 1)))))

;; (defn save-game-round
;;   [game-round]
;;   (mc/insert db rounds-coll game-round))

;; (defn get-game-round-count
;;   [game-id]
;;   (mc/count db rounds-coll {:game-id (ObjectId. game-id)}))

;; (defn get-game-round
;;   [game-id round-number]
;;   (mc/find-one-as-map db rounds-coll {:game-id (ObjectId. game-id)
;;                                       :round (Integer/parseInt round-number)}))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PLAYER OPERATIONS
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^{:private true} player-coll "players")
(def ^{:private true} player-fields [:admin
                                     :avatar_url
                                     :email
                                     :login
                                     :github-id
                                     :name
                                     :bots])

(defn get-all-players
  "Returns app players using projection"
  []
  (q/with-collection db player-coll
    (q/find {})
    (q/fields player-fields)))

(defn get-player
  "Returns a player matching a given player-id"
  [player-id]
  (first (q/with-collection db player-coll
           (q/find {:_id (ObjectId. player-id)})
           (q/limit 1)
           (q/fields player-fields))))

(defn get-player-with-auth-token
  "Returns a player object and exposes the auth token. WARNING: Should not
  be used even for admins. This is only used by the game engine to retrieve
  bots that are hosted on private repos."
  [player-id]
  (first (q/with-collection db player-coll
           (q/find {:_id (ObjectId. player-id)})
           (q/limit 1))))

(defn add-or-update-player
  "Create or Update player"
  [player]
  (mc/save-and-return db player-coll player))

(defn get-player-by-github-id
  "Querys the player collection for a players github id"
  [github-id]
  (first (q/with-collection db player-coll
           (q/find {:github-id github-id})
           (q/limit 1)
           (q/fields player-fields))))

(defn get-player-by-auth-token
  "NOTE: get-player-by-auth-token is used by the middleware layer
  and exposes the entire player map"
  [access-token]
  (mc/find-one-as-map db player-coll {:access-token access-token}))

(defn remove-player
  "Removes a player"
  [player-id]
  (mc/remove-by-id db player-coll (ObjectId. player-id)))

(defn add-player-bot
  "Adds a bot to a player"
  [player-id bot]
  (mc/update db player-coll {:_id (ObjectId. player-id)} {$push {:bots bot}}))

(defn remove-player-bot
  "Removes a bot from a player"
  [player-id repo]
  (mc/update db player-coll {:_id (ObjectId. player-id)} {$pull {:bots {:repo repo}}}))
