(ns wombats.constants)

(defonce github-base "https://github.com/login/oauth/")
(defonce github-authorize-url (str github-base "authorize"))
(defonce github-access-token-url (str github-base "access_token"))
(defonce github-scopes "user:email")
(defonce github-user-profile-url "https://api.github.com/user")
(defonce github-repo-api-base "https://api.github.com/repos/")
