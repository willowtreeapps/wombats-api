(ns wombats-api.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [compojure.route :as route]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [wombats-api.routes.services :refer [service-routes]]
            [wombats-api.middleware :as middleware]
            [wombats-api.env :refer [defaults]]
            [wombats-api.routes.rules :refer [rules]]
            [mount.core :as mount]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    #'service-routes
    (route/not-found
      "page not found")))

(defn app [] (middleware/wrap-base (wrap-access-rules #'app-routes rules)))
