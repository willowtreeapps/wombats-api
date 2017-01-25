(ns wombats.handlers.static-pages
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [clojure.core.async :refer [chan go >!]]))

(def home-page
  (interceptor/before
   ::home-page
   (fn [{:keys [response] :as context}]
     (let [ch (chan 1)]
       (go
         (>! ch (assoc context :response (assoc response
                                                :status 200
                                                :body "Wombats!!!!"))))
       ch))))
