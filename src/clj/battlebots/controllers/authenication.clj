(ns battlebots.controllers.authenication
  (:require [ring.util.response :refer [response]]
            [buddy.auth :refer [throw-unauthorized]]
            [buddy.sign.jws :as jws] 
            [buddy.hashers :as hashers]
            [battlebots.services.mongodb :as db]
            [battlebots.middleware :refer [secret]]
            [battlebots.controllers.players :refer [players-coll]]
            [battlebots.schemas.player :refer [isPlayer]]))

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
    (response (dissoc (db/insert-one players-coll player-object) :password))))

(defn login
  "login handler"
  [login]
  (let [user (db/find-one-by players-coll :username (:username login))
        token {:token (jws/sign (select-keys user [:_id
                                                   :username
                                                   :bot-repo
                                                   :roles]) secret)}
        isAuthed? (hashers/check (:password login) (:password user))]
    (if isAuthed? 
      (response token)
      (throw-unauthorized {:message "Not authorized"}))))
