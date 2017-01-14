(ns wombats.handlers.user
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]
            [wombats.daos.core :as dao]))

(def ^:private users [])

(defbefore get-users
  "Returns a seq of users"
  [{:keys [response] :as context}]
  (let [ch (chan 1)
        get-users (dao/get-fn :get-users context)]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body (get-users)))))
    ch))

(defbefore post-user
  "Adds a new user to the db"
  [{:keys [response] :as context}]
  (let [ch (chan 1)
        add-user (dao/get-fn :add-user context)]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body @(add-user {:username "emily"})))))
    ch))
