(ns battlebots.controllers.authenication
  (:require [ring.util.response :refer [response redirect]]
            [clojure.string :as string]
            [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]
            [battlebots.services.mongodb :as db]
            [battlebots.controllers.players :refer [players-coll]]
            [battlebots.schemas.player :refer [isPlayer]]))

;; GITHUB OAUTH
(def github-base "https://github.com/login/oauth/")
(def github-authorize-url (str github-base "authorize"))
(def github-access-token-url (str github-base "access_token"))
(def user-profile-url "https://api.github.com/user")
(def user-repos-url "https://api.github.com/user/repos")
(def client-id (System/getenv "WT_BATTLEBOTS_GITHUB_CLIENT_ID_DEV"))
(def client-secret (System/getenv "WT_BATTLEBOTS_GITHUB_CLIENT_SECRET_DEV"))
(def signing-secret (System/getenv "WT_BATTLEBOTS_OAUTH_SIGNING_SECRET"))

(defn account-details
  "returns the current logged in users player object"
  [request]
  (let [user (:identity request)]
    (response user)))

(defn signin
  "Signs a user into battlebots using GitHub OAuth2 service"
  []
  (redirect (str github-authorize-url
                 "?client_id=" client-id
                 "&scope=" "user public_repo"
                 "&state=" signing-secret)))

(defn update-user
  "insert / updates a user record"
  [{:keys [github-id] :as profile}]
  (let [user (merge (db/find-one-by players-coll :github-id github-id) profile)]
    (db/save players-coll user)))

(defn parse-profile
  "Parse user profile"
  [full-profile access-token]
  (let [profile (select-keys full-profile [:email :name :login :avatar_url])]
    (merge profile {:github-id (:id full-profile)
                    :access-token access-token})))

(defn parse-repos
  "Parse repo vector"
  [repos]
  (map #(select-keys % [:full_name :downloads_url]) repos))

(defn build-profile
  "Makes requests to GitHub to create a user profile"
  [access-token]
  (let [headers {"Authorization" (str "token " access-token)
                 "Accept" "application/json"}
        {:keys [body status]} @(http/get user-profile-url {:headers headers})]
    (parse-profile (parse-string body true) access-token)))

(defn process-user
  "Process the OAuth2 response"
  [{:keys [code state]}]
  (if (= state signing-secret)
    (http/post github-access-token-url
               {:query-params {:client_id client-id
                               :client_secret client-secret
                               :code code}
                :headers {"Accept" "application/json"}}
               (fn [{:keys [body status]}]
                 (let [access-token (:access_token (parse-string body true))
                       profile (build-profile access-token)]
                   (update-user profile)
                   (redirect (str "/?access-token=" access-token)))))
    (redirect "/")))
