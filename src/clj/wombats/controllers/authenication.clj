(ns wombats.controllers.authenication
  (:require [ring.util.response :refer [response redirect]]
            [clojure.string :as string]
            [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]
            [wombats.services.mongodb :as db]
            [wombats.schemas.player :refer [isPlayer]]))

;; GITHUB OAUTH
(def github-base "https://github.com/login/oauth/")
(def github-authorize-url (str github-base "authorize"))
(def github-access-token-url (str github-base "access_token"))
(def user-profile-url "https://api.github.com/user")
(def user-repos-url "https://api.github.com/user/repos")
(def client-id (System/getenv "WOMBATS_GITHUB_CLIENT_ID"))
(def client-secret (System/getenv "WOMBATS_GITHUB_CLIENT_SECRET"))
(def signing-secret (System/getenv "WOMBATS_OAUTH_SIGNING_SECRET"))
(def web-client-url (System/getenv "WOMBATS_WEB_CLIENT_URL"))

(if-not (and client-id client-secret signing-secret)
  (throw (Exception. "Missing OAuth Environment Variable. Check out the README for more information.")))

(if-not web-client-url
  (throw (Exception. "Missing Web Client URL Environment Variable.")))

;; The base player map ensure all new users will contain these values
(def base-player
  {:bots []})

(defn account-details
  "returns the current logged in users player object"
  [request]
  (let [user (:identity request)]
    (response user)))

(defn signin
  "Signs a user into wombats using GitHub OAuth2 service"
  []
  (redirect (str github-authorize-url
                 "?client_id=" client-id
                 "&scope=" "user public_repo"
                 "&state=" signing-secret)))

(defn update-user
  "insert / updates a user record"
  [{:keys [github-id] :as profile}]
  (let [player (merge base-player (merge (db/get-player-by-github-id github-id) profile))]
    (db/add-or-update-player player)))

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
                   (redirect (str web-client-url "?access-token=" access-token)))))
    (redirect web-client-url)))
