(ns wombats-api.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[wombats-api started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[wombats-api has shut down successfully]=-"))
   :middleware identity})
