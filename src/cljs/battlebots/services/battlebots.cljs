(ns battlebots.services.battlebots
  (:require [ajax.core :refer [GET POST DELETE]]
            [battlebots.services.utils :refer [add-auth-header]]))

;;
;; Account
;;
(defn get-current-user
  "fetches the current user object"
  [on-success on-error]
  (GET "/auth/account-details" {:response-format :json
                                :keywords? true
                                :headers (add-auth-header {})
                                :handler on-success
                                :error-handler on-error}))

(defn post-credentials 
  "Authenticates a user's credentials"
  [user-credentials on-success on-error]
  (POST "/auth/login" {:response-format :json
                       :keywords? true
                       :params user-credentials
                       :handler on-success
                       :error-handler on-error}))

(defn post-new-credentials
  "Creates a new user and then authenticates"
  [user-credentials on-success on-error]
  (POST "/auth/signup" {:response-format :json
                        :keywords? true
                        :params user-credentials
                        :handler on-success
                        :error-handler on-error}))

;;
;; Game
;;
(defn get-games
  "fetches a list of all available games"
  [on-success on-error]
  (GET "/games" {:response-format :json
                 :keywords? true
                 :headers (add-auth-header {})
                 :handler on-success
                 :error-handler on-error}))

(defn post-game
  "creates a new game record"
  [on-success on-error]
  (POST "/games" {:response-format :json
                  :keywords? true
                  :headers (add-auth-header {})
                  :handler on-success
                  :error-handler on-error}))

(defn del-game
  "deletes a game record"
  [_id on-success on-error]
  (DELETE (str "/games/" _id) {:keywords? true
                               :headers (add-auth-header {})
                               :handler on-success
                               :error-handler on-error}))
