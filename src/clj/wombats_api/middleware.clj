(ns wombats-api.middleware
  (:require [wombats-api.env :refer [defaults]]
            [wombats-api.config :refer [env]]
            [wombats-api.db.core :as db]
            [wombats-api.routes.rules :as access-rules]
            [ring-ttl-session.core :refer [ttl-memory-store]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.cors :refer [wrap-cors]]
            [buddy.auth.middleware :refer [wrap-authentication
                                           wrap-authorization]]
            [buddy.auth.backends :as backends]))

(defn auth-token
  [request token]
  (let [user (db/get-player-by-auth-token token)]
    (when user
      (merge user {:_id (str (:_id user))}))))

(def backend (backends/token {:authfn auth-token}))

(defn wrap-auth [handler]
   (-> handler
       (wrap-authentication backend)
       (wrap-authorization backend)))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))))
