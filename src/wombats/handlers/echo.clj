(ns wombats.handlers.echo
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [clojure.core.async :refer [chan go >!]]))

(def echo
  "Simple echo handler"
  (interceptor/before
   ::echo
   (fn [{:keys [request response] :as context}]
     (let [ch (chan 1)]
       (go
         (>! ch (assoc context :response (assoc response
                                                :status 200
                                                :body request))))
       ch))))
