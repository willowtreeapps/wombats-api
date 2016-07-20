(ns battlebots.middleware
  (:require [ring.middleware.defaults :refer [api-defaults site-defaults wrap-defaults]]
            [prone.middleware :refer [wrap-exceptions]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.middleware.transit :refer [wrap-transit-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [battlebots.services.mongodb :as db]
            [monger.json]))

;; https://blog.8thlight.com/mike-knepper/2015/05/19/handling-exceptions-with-middleware-in-clojure.html
(defn wrap-exception-handling
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (.printStackTrace e)
        {:status 400 :body (.getMessage e)}))))

(defn auth-user
  "attaches a user object to a req"
  [request token]
  (let [user (db/get-player-by-auth-token token)]
    (when user
      (merge user {:_id (str (:_id user))}))))

(def backend (backends/token {:authfn auth-user}))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults api-defaults) ;; api-defaults should only be set for api endpoints. TODO refactor out site endpoints
      wrap-json-params
      wrap-transit-params
      wrap-json-response
      (wrap-authorization backend)
      (wrap-authentication backend)
      wrap-exception-handling
      ;; wrap-exceptions
      wrap-reload))
