(ns wombats.handlers.auth
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]
            [org.httpkit.client :as http]
            [cheshire.core :refer [parse-string]]
            [wombats.interceptors.github :refer [get-github-settings]]))

(def ^:private github-base "https://github.com/login/oauth/")
(def ^:private github-authorize-url (str github-base "authorize"))
(def ^:private github-access-token-url (str github-base "access_token"))
(def ^:private user-profile-url "https://api.github.com/user")
(def ^:private user-repos-url "https://api.github.com/user/repos")

(def ^:private github-scopes "user:email")

(defn- get-access-token
  "POST request to get a GitHub OAuth token.
  If no auth token is found, nil will return"
  [request-params]
  (http/post github-access-token-url
             request-params
             (fn [{:keys [body]}]
               (get (parse-string body true) :access_token nil))))

(defn- redirect-home
  "Final redirect home with or without access token appended"
  ([context]
   (assoc context :response (assoc (:response context)
                                   :headers {"Location" "/"}
                                   :status 302)))
  ([context access-token]
   (assoc context :response (assoc (:response context)
                                   :headers {"Location" (str "/?access-token=" access-token)}
                                   :status 302))))

;; TODO signing secret should not be consistant across requests. Gen uuid to send.
(defbefore github-redirect
  [{:keys [response] :as context}]
  (let [ch (chan 1)
        {:keys [client-id signing-secret]} (get-github-settings context)
        github-redirect (str github-authorize-url
                             "?client_id=" client-id
                             "&scope=" github-scopes
                             "&state=" signing-secret)]
    (go
      (>! ch (assoc context :response (assoc response
                                             :headers {"Location" github-redirect}
                                             :status 302
                                             :body nil))))
    ch))

(defbefore github-callback
  [{:keys [request response] :as context}]
  (let [ch (chan 1)
        {:keys [client-id client-secret signing-secret]} (get-github-settings context)
        {:keys [code state]} (:query-params request)]
    (go
      (if (= state signing-secret)
        (let [access-token @(get-access-token {:query-params {:client_id client-id
                                                              :client_secret client-secret
                                                              :code code}
                                               :headers {"Accept" "application/json"}})]
          (if access-token
            (>! ch (redirect-home context access-token))
            (>! ch (redirect-home context))))
        (>! ch (redirect-home context))))
    ch))
