(ns wombats-api.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [wombats-api.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[wombats-api started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[wombats-api has shut down successfully]=-"))
   :middleware wrap-dev})
