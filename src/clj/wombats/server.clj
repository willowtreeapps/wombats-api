(ns wombats.server
  (:use org.httpkit.server)
  (:require [wombats.router :refer [app]]
            [wombats.services.mongodb :refer [initialize-db]]
            [config.core :refer [env]])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (initialize-db)
    (run-server app {:port port
                    :join? false})))
