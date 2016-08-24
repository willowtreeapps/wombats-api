(ns wombats-api.routes.rules
  (:require [clojure.string :refer [includes?]]
            [buddy.auth :refer [authenticated?]]))

;;
;; Helper functions
;;
(defn get-user
  "Pull the user id out of the identity map"
  [request]
  (:identity request))

;;
;; Access Handlers
;;
;; Access handlers allow you to define rules around who is allowed
;; access to specified resources

(defn any-access "Un-restricted access to resources" [request] true)

(defn authenticated-user
  "Checks to ensure a user is logged in but makes no assumption of role"
  [request]
  (authenticated? request))

(defn is-user?
  "Determins if a give request is from an authorized user"
  [request]
  (boolean (get-user request)))

(defn is-admin?
  [request]
  (:admin (get-user request)))

(defn is-current-user?
  "Determins if the user making the request is altering their own resource(s)

  TODO This is not the best, however it get's the job done for now.

  We return true if one of the following holds true

  1. The users id is contained in the URI
  2. The users id is contained in the body map as `_id`
  "
  [request]
  (let [user-id (:_id (get-user request))
        contains-id-in-uri? (includes? (:uri request) user-id)
        contains-id-in-params? (= user-id (get-in request [:params :_id]))]
    (or contains-id-in-uri? contains-id-in-params?)))

;;
;; Access Rules
;;
;; Access rules define the auth logic that protects each endpoint.
;; By default if no rule is specified then access is allowed (This
;; can be adjusted)
;;
;; NOTE: Access Rules do not cascade. The first match will resolve
;; the request
(def rules {:rules [] #_[ ;; Authentication
                         {:uri "/api/v1/auth/account-details"
                          :handler authenticated-user}

                         {:pattern #"^/api/v1/auth/.*"
                          :handler any-access}

                         ;; Games
                         {:uris ["/api/v1/game/:game-id{\\w+}/start"]
                          :handler is-admin?}

                         {:pattern #"^/api/v1/game/.*"
                          :handler is-admin?
                          :request-method :delete}

                         {:uri "/api/v1/game/:game-id{\\w+}/player/:player-id{\\w+}"
                          :handler is-current-user?
                          :request-method :post}

                         {:pattern #"^/api/v1/game.*"
                          :handler authenticated-user}

                         {:pattern #"^/api/v1/simulator.*"
                          :handler authenticated-user}

                         {:uris ["/api/v1/player/:player-id{\\w+}/bot"
                                 "/api/v1/player/:player-id{\\w+}/bot/:repo{\\w+}"]
                          :handler is-current-user?}

                         ;; Players
                         {:pattern #"^/api/v1/player.*"
                          :handler is-admin?}]})
