(ns wombats.handlers.auth
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]
            [wombats.interceptors.github :refer [get-github-settings]]
            [wombats.daos.helpers :as dao]))

(def ^:private github-base "https://github.com/login/oauth/")
(def ^:private github-authorize-url (str github-base "authorize"))
(def ^:private github-access-token-url (str github-base "access_token"))
(def ^:private github-scopes "user:email")
(def ^:private github-user-profile-url "https://api.github.com/user")

;; Helper Functions

(defn- get-access-token
  "POST request to get a GitHub OAuth token.
  If no auth token is found, nil will return"
  [query-params]
  (http/post github-access-token-url
             {:query-params query-params
              :headers {"Accept" "application/json"}}
             (fn [{:keys [body]}]
               (get (parse-string body true) :access_token nil))))

(defn- get-github-user
  "Fetches the github user attached to a given access token"
  [access-token]
  (http/get github-user-profile-url
            {:headers {"Authorization" (str "token " access-token)
                       "Accept" "application/json"}}))

(defn- remove-slash
  "Removes the trailing slash from a url (if it exists)"
  [url]
  (if (.endsWith url "/")
    (.substring url 0 (- (count url) 1))
    url))

(defn- parse-user-response
  "Parses the body if the request succeeded"
  [{:keys [body status]}]
  (when (= status 200)
    (parse-string body true)))

(defn- redirect-home
  "Final redirect home with or without access token appended"
  ([context redirect]
   (assoc context :response (assoc (:response context)
                                   :headers {"Location" redirect}
                                   :status 302)))
  ([context redirect access-token]
   (assoc context :response (assoc (:response context)
                                   :headers {"Location" (str redirect "?access-token=" access-token)}
                                   :status 302))))

;; Handlers

(def ^:swagger-spec signin-spec
  {"/api/v1/auth/github/signin"
   {:get {:description "Gets authorization and access token"
          :tags ["auth"]
          :operationId "signout"
          :response {:302 {:description "signout response"}}}}})

;; TODO signing secret should not be consistant across requests. Gen uuid to send.
(def signin
  "Signin handler"
  (interceptor/before
   ::sign-in
   (fn [{:keys [response] :as context}]
     (let [{:keys [client-id signing-secret]} (get-github-settings context)
           github-redirect (str github-authorize-url
                                "?client_id=" client-id
                                "&scope=" github-scopes
                                "&state=" signing-secret)]

       (assoc context :response (assoc response
                                       :headers {"Location" github-redirect}
                                       :status 302
                                       :body nil))))))

(def github-callback
  "Callback handler from GitHub OAuth request"
  (interceptor/before
   ::github-callback
   (fn [{:keys [request response] :as context}]
     (let [{:keys [client-id
                   client-secret
                   signing-secret]} (get-github-settings context)
           {:keys [code state]} (:query-params request)
           web-client-redirect (remove-slash (get-in request [:headers "referer"]))
           failed-callback (redirect-home context web-client-redirect)]

       (if (= state signing-secret)
         (let [access-token @(get-access-token {:client_id client-id
                                                :client_secret client-secret
                                                :code code})
               ;; TODO BUG If the user does not have their email setup on GH this
               ;; step will fail
               user (when access-token
                      (parse-user-response @(get-github-user access-token)))
               create-or-update-user (dao/get-fn :create-or-update-user context)
               get-user-by-email (dao/get-fn :get-user-by-email context)]
           (if (and access-token user)
             (let [user-update (select-keys user [:email :login :id :avatar_url])
                   current-user (get-user-by-email (:email user-update))
                   updated-user @(create-or-update-user user-update
                                                        access-token
                                                        (:user/id current-user))]
               (redirect-home context web-client-redirect access-token))
             failed-callback))
         failed-callback)))))

(def ^:swagger-spec signout-spec
  {"/api/v1/auth/github/signout"
   {:get {:description "Removes auth token from db and redirects to home."
          :tags ["auth"]
          :operationId "signout"
          :response {:302 {:description "signout response"}}}}})

(def signout
  "Signout handler"
  (interceptor/before
   ::signout
   (fn [{:keys [request response] :as context}]
     (let [access-token (get-in request [:headers "authorization"])
           remove-access-token (dao/get-fn :remove-access-token context)
           web-client-redirect (remove-slash (get-in request [:headers "referer"]))]

       (when access-token
         @(remove-access-token access-token))

       (assoc context :response (assoc response
                                       :headers {"Location" web-client-redirect}
                                       :status 302
                                       :body nil))))))
