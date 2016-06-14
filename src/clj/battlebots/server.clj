(ns battlebots.server
  (:use org.httpkit.server)
  (:require [battlebots.router :refer [app]]
            [config.core :refer [env]])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (run-server app {:port port
                    :join? false})))
