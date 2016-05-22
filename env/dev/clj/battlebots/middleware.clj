(ns battlebots.middleware
  (:require [ring.middleware.defaults :refer [api-defaults site-defaults wrap-defaults]]
            [prone.middleware :refer [wrap-exceptions]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.transit :refer [wrap-transit-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [monger.json]))

;; https://blog.8thlight.com/mike-knepper/2015/05/19/handling-exceptions-with-middleware-in-clojure.html
(defn wrap-exception-handling
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 400 :body (.getMessage e)}))))

;; TODO pull secret from ENV
(def secret "mysecret")
(def backend (backends/jws {:secret secret}))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults api-defaults) ;; api-defaults should only be set for api endpoints. TODO refactor out site endpoints
      wrap-transit-params
      wrap-json-response
      (wrap-authorization backend)
      (wrap-authentication backend)
      wrap-exception-handling
      ;; wrap-exceptions
      wrap-reload))
