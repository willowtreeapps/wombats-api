(ns wombats.handlers.static-pages
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]))

(defbefore home-page [{:keys [response] :as context}]
  (let [ch (chan 1)]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body "Wombats!!!!"))))
    ch))
