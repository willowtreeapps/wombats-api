(ns battlebots.services.battlebots
  (:require [ajax.core :refer [GET POST DELETE]]
            [battlebots.services.utils :refer [add-auth-header]]))

;;
;; Account
;;
(defn get-current-user
  "fetches the current user object"
  [on-success on-error]
  (GET "/api/v1/auth/account-details" {:response-format :json
                                       :keywords? true
                                       :headers (add-auth-header {})
                                       :handler on-success
                                       :error-handler on-error}))

(defn post-credentials
  "Authenticates a user's credentials"
  [user-credentials on-success on-error]
  (POST "/api/v1/auth/login" {:response-format :json
                              :keywords? true
                              :params user-credentials
                              :handler on-success
                              :error-handler on-error}))

(defn post-new-credentials
  "Creates a new user and then authenticates"
  [user-credentials on-success on-error]
  (POST "/api/v1/auth/signup" {:response-format :json
                               :keywords? true
                               :params user-credentials
                               :handler on-success
                               :error-handler on-error}))

(defn post-new-bot
  "Adds a bot repo to a users account"
  [bot player-id on-success on-error]
  (POST (str "/api/v1/player/" player-id "/bot") {:response-format :json
                                                  :keywords? true
                                                  :headers (add-auth-header {})
                                                  :params bot
                                                  :handler on-success
                                                  :error-handler on-error}))

(defn delete-player-bot
  "Remove a bot repo from a users account"
  [repo player-id on-success on-error]
  (DELETE (str "/api/v1/player/" player-id "/bot/" repo) {:response-format :json
                                                          :keywords? true
                                                          :headers (add-auth-header {})
                                                          :handler on-success
                                                          :error-handler on-error}))

;;
;; Game
;;
(defn get-games
  "fetches a list of all available games"
  [on-success on-error]
  (GET "/api/v1/game" {:response-format :json
                       :keywords? true
                       :headers (add-auth-header {})
                       :handler on-success
                       :error-handler on-error}))

(defn post-game
  "creates a new game record"
  [on-success on-error]
  (POST "/api/v1/game" {:response-format :json
                        :keywords? true
                        :headers (add-auth-header {})
                        :handler on-success
                        :error-handler on-error}))

(defn post-game-user
  "adds a user record to a game"
  [game-id user-id on-success on-error]
  (POST (str "/api/v1/game/" game-id "/player/" user-id) {:response-format :json
                                                          :keywords? true
                                                          :headers (add-auth-header {})
                                                          :handler on-success
                                                          :error-handler on-error}))

(defn del-game
  "deletes a game record"
  [_id on-success on-error]
  (DELETE (str "/api/v1/game/" _id) {:keywords? true
                                     :headers (add-auth-header {})
                                     :handler on-success
                                     :error-handler on-error}))

(defn post-game-initialize
  "updates game state from pending to initialized"
  [game-id on-success on-error]
  (POST (str "api/v1/game/" game-id "/initialize") {:response-format :json
                                                    :keywords? true
                                                    :headers (add-auth-header {})
                                                    :handler on-success
                                                    :error-handler on-error}))

(defn post-game-start
  "updates game state from initialized to started"
  [game-id on-success on-error]
  (POST (str "api/v1/game/" game-id "/start") {:response-format :json
                                               :keywords? true
                                               :headers (add-auth-header {})
                                               :handler on-success
                                               :error-handler on-error}))

;;
;; Players / Users
;;
(defn get-users
  "fetches a list of all players"
  [on-success on-error]
  (GET "/api/v1/player" {:response-format :json
                         :keywords? true
                         :headers (add-auth-header {})
                         :handler on-success
                         :error-handler on-error}))

(defn del-user
  "removes a user record"
  [user-id on-success on-error]
  (DELETE (str "api/v1/player/" user-id) {:keywords? true
                                          :headers (add-auth-header {})
                                          :handler on-success
                                          :error-handler on-error}))
