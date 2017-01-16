(ns wombats.handlers.auth
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]
            [wombats.interceptors.github :refer [get-github-settings]]))

(def ^:private github-base "https://github.com/login/oauth/")
(def ^:private github-authorize-url (str github-base "authorize"))
(def ^:private github-access-token-url (str github-base "access_token"))
(def ^:private user-profile-url "https://api.github.com/user")
(def ^:private user-repos-url "https://api.github.com/user/repos")

(def ^:private github-scopes "user:email")

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
                                             :body ""))))
    ch))

(defbefore github-callback
  [{:keys [request response] :as context}]
  (let [ch (chan 1)
        {:keys [client-id client-secret]} (get-github-settings context)
        {:keys [code state]} (:query-params request)
        github-access-token-endpoint (str github-access-token-url
                                          "?client_id=" client-id
                                          "&client_secret=" client-secret
                                          "&code=" code)]
    (go
      ;; TODO Fetch auth code
      (>! ch (assoc context :response (assoc response
                                             :headers {"Content-Type" "text/html"}
                                             :status 200
                                             :body "Callback"))))
    ch))
