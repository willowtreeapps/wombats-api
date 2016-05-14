(ns battlebots.middleware
  (:require [ring.middleware.defaults :refer [api-defaults site-defaults wrap-defaults]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.reload :refer [wrap-reload]]
            [monger.json]))

(defn wrap-middleware [handler]
  (-> handler
     (wrap-defaults api-defaults) ;; api-defaults should only be set for api endpoints. TODO refactor out site endpoints
      wrap-json-body
      wrap-json-response
      wrap-exceptions
      wrap-reload))
