(ns wombats-api.controllers.authenication
  (:require [ring.util.response :refer [response redirect]]
            [clojure.string :as string]
            [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]
            [wombats-api.config :refer [env]]
            [wombats-api.db.core :as db]))

;; Github configuration variables
(def ^{:private true} github-base "https://github.com/login/oauth/")
(def ^{:private true} github-authorize-url (str github-base "authorize"))
(def ^{:private true} github-access-token-url (str github-base "access_token"))
(def ^{:private true} user-profile-url "https://api.github.com/user")
(def ^{:private true} user-repos-url "https://api.github.com/user/repos")

;; The base player map ensure all new users will contain these values
(def ^{:private true} base-player {:bots []})

(defn- update-user
  "Insert / Updates a user record"
  [{:keys [github-id] :as profile}]
  (let [player (merge base-player (merge (db/get-player-by-github-id github-id) profile))]
    (db/add-or-update-player player)))

(defn- parse-profile
  "Parse user profile"
  [full-profile access-token]
  (let [profile (select-keys full-profile [:email :name :login :avatar_url])]
    (merge profile {:github-id (:id full-profile)
                    :access-token access-token})))

(defn- build-profile
  "Makes requests to GitHub to create a user profile"
  [access-token]
  (let [headers {"Authorization" (str "token " access-token)
                 "Accept" "application/json"}
        {:keys [body status]} @(http/get user-profile-url {:headers headers})]
    (parse-profile (parse-string body true) access-token)))

;; ;;;;;;;;;;;;;;;;;;;;;;
;; Handlers
;; ;;;;;;;;;;;;;;;;;;;;;;

(defn github-signin-route
  []
  (str github-authorize-url
       "?client_id=" (:wombats-github-client-id env)
       "&scope=" "user public_repo"
       "&state=" (:wombats-oauth-signing-secret env)))

(defn process-user
  [code state]
  (if (= state (:wombats-oauth-signing-secret env))
    @(http/post
      github-access-token-url
      {:query-params {:client_id (:wombats-github-client-id env)
                      :client_secret (:wombats-github-client-secret env)
                      :code code}
       :headers {"Accept" "application/json"}}
      (fn [{:keys [body status]}]
        (let [access-token (:access_token (parse-string body true))
              profile (build-profile access-token)]
          (update-user profile)
          (str (:wombats-web-client-url env) "?access-token=" access-token))))
    (str (:wombats-web-client-url env))))
