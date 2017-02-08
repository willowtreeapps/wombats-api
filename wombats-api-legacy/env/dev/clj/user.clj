(ns user
  (:require [mount.core :as mount]
            wombats-api.core))

(defn start []
  (mount/start-without #'wombats-api.core/repl-server))

(defn stop []
  (mount/stop-except #'wombats-api.core/repl-server))

(defn restart []
  (stop)
  (start))


