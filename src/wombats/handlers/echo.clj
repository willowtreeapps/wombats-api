(ns wombats.handlers.echo
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]))

(defbefore echo [{:keys [request response] :as context}]
  (let [ch (chan 1)]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body request))))
    ch))
