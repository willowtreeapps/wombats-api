(ns battlebots.controllers.authenication
  (:require [ring.util.response :refer [response]]
            [buddy.auth :refer [throw-unauthorized]]
            [buddy.sign.jws :as jws] 
            [buddy.hashers :as hashers]
            [battlebots.services.mongodb :as db]
            [battlebots.middleware :refer [secret]]
            [battlebots.controllers.players :refer [players-coll]]
            [battlebots.schemas.player :refer [isPlayer]]))

(defn user-to-token
  "generates a token from a given user object"
  [user]
  {:token (jws/sign (select-keys user [:_id
                                       :username
                                       :bot-repo
                                       :roles]) secret)})

(defn account-details
  "returns the current logged in users player object"
  [request]
  (let [_id (get-in request [:identity :_id])]
    (response (dissoc (db/find-one players-coll _id) :password))))

(defn signup
  "user signup"
  [player]
  (let [player-object (merge (isPlayer player) {:password (hashers/derive (:password player))
                                                :roles [:user]})]
    (let [user (db/insert-one players-coll player-object)]
      (response (user-to-token user)))))

(defn login
  "login handler"
  [login]
  (let [user (db/find-one-by players-coll :username (:username login))
        isAuthed? (hashers/check (:password login) (:password user))]
    (if isAuthed? 
      (response (user-to-token user))
      (throw-unauthorized {:message "Not authorized"}))))
