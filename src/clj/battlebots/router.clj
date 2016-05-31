(ns battlebots.router
  (:require [compojure.core :refer [GET POST DELETE context defroutes]]
            [compojure.route :refer [not-found resources]]
            [clojure.string :refer [includes?]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [wrap-access-rules success error]]
            [battlebots.middleware :refer [wrap-middleware]]
            [battlebots.controllers.games :as games]
            [battlebots.controllers.players :as players]
            [battlebots.controllers.authenication :as auth]
            [battlebots.views.index :refer [index]]
            [battlebots.utils :refer [in?]]))

;;
;; Helper functions
;;
(defn get-user-id
  "Pull the user id out of the identity map"
  [request]
  (get-in request [:identity :_id]))

(defn get-roles
  "Pulls rolls out of a request object"
  [request]
  (get-in request [:identity :roles]))

(defn contains-role?
  "Determins if a given role is present in a users role vector"
  [request role]
  (in? (get-roles request) role))

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
  (contains-role? request "user"))

(defn is-admin?
  "Determins if a given request is from an authorized admin"
  [request]
  (contains-role? request "admin"))

(defn isCurrentUser?
  "Determins if the user making the request is altering their own resource(s)

  TODO This is not the best, however it get's the job done for now.

  We return true if one of the following holds true

  1. The users id is contained in the URI
  2. The users id is contained in the body map as `_id`
  "
  [request]
  (let [user-id (get-user-id request)
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
(def access-rules {:rules [;; Authenication
                           {:uri "/api/v1/auth/account-details"
                            :handler authenticated-user}

                           {:pattern #"^/api/v1/auth/.*"
                            :handler any-access}

                           ;; Games
                           {:pattern #"^/api/v1/game/.*"
                            :handler is-admin?
                            :request-method :delete}

                           {:uri "/api/v1/game/:game-id{\\w+}/player/:player-id{\\w+}"
                            :handler isCurrentUser?
                            :request-method :post}

                           {:pattern #"^/api/v1/game.*"
                            :handler authenticated-user}

                           ;; Players
                           {:pattern #"^/api/v1/player.*"
                            :handler is-admin?}]})

;;
;; Route Deffinitions
;;
(defroutes
  routes

  (context "/api/v1" []
    (context "/game" []
      (GET "/" [] (games/get-games))
      (POST "/" [] (games/add-game))

      (context "/:game-id" [game-id]
        (GET "/" [] (games/get-games game-id))
        (DELETE "/" [] (games/remove-game game-id))

        (context "/round" []
          (GET "/" [] (games/get-rounds game-id))
          (POST "/" [] (games/add-round game-id))

          (context "/:round-id" [round-id]
            (GET "/" [] (games/get-rounds game-id round-id))))

        (context "/player" []
          (GET "/" [] (games/get-players game-id))

          (context "/:player-id" [player-id]
            (GET "/" [] (games/get-players game-id player-id))
            (POST "/" req (games/add-player game-id (:identity req)))))))

    (context "/player" []
      (GET "/" []  (players/get-players))

      (context "/:player-id" [player-id]
        (GET "/" [] (players/get-players player-id))
        (DELETE "/" [] (players/remove-player player-id))))

    (context "/auth" []
      (GET "/account-details" req (auth/account-details req))
      (POST "/signup" req (auth/signup (:params req)))
      (POST "/login" req (auth/login (:params req)))))

  ;; Main view
  (GET "/" [] index)

  ;; Resources servered from this route
  (resources "/")

  ;; No resource found
  (not-found "Not Found")

  (def app (wrap-middleware (wrap-access-rules #'routes access-rules))))
