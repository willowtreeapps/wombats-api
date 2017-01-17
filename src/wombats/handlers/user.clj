(ns wombats.handlers.user
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]
            [wombats.daos.core :as dao]))

(def ^{:swagger-spec true} get-users-spec
  {"/api/v1/users"
   {:get {:description "Returns all users"
          :tags ["user"]
          :operationId "get-users"
          :responses {:200 {:description "get-users response"}}}}})
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

(def ^{:swagger-spec true} get-user-by-id-spec
  {"/api/v1/users/id/{user-id}"
   {:get {:description "Returns a user matching a given id"
          :tags ["user"]
          :operationId "get-user-by-id"
          :responses {:200 {:description "get-user-by-id response"}}}}})
(defbefore get-user-by-id
  "Returns a user by searching for its id"
  [{:keys [response request] :as context}]
  (let [ch (chan 1)
        get-user-by-id (dao/get-fn :get-user-by-id context)
        user-id (get-in request [:path-params :user-id])]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body (get-user-by-id user-id)))))
    ch))

(defbefore get-user-by-email
  "Returns a user by searching for its email"
  [{:keys [response request] :as context}]
  (let [ch (chan 1)
        get-user-by-email (dao/get-fn :get-user-by-email context)
        email (get-in request [:path-params :email])]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body (get-user-by-email email)))))
    ch))

(defbefore get-user-by-access-token
  "Returns a user by searching for its access token"
  [{:keys [response request] :as context}]
  (let [ch (chan 1)
        get-user-by-token (dao/get-fn :get-user-by-access-token context)
        access-token (get-in request [:path-params :access-token])]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body (get-user-by-token access-token)))))
    ch))

(defbefore get-user-wombats
  "Returns a seq of user wombats"
  [{:keys [response request] :as context}]
  (let [ch (chan 1)
        get-user-wombats (dao/get-fn :get-user-wombats context)
        user-id (get-in request [:path-params :user-id])]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body (get-user-wombats user-id)))))
    ch))

(defbefore add-user-wombat
  "Creates a new wombat and assigns it to the user"
  [{:keys [response request] :as context}]
  (let [ch (chan 1)
        add-user-wombat (dao/get-fn :add-user-wombat context)
        get-wombat-by-name (dao/get-fn :get-wombat-by-name context)
        wombat (:edn-params request)
        user-id (get-in request [:path-params :user-id])]
    (go
      (let [tx @(add-user-wombat user-id wombat)
            wombat-record (get-wombat-by-name (:name wombat))]
        (>! ch (assoc context :response (assoc response
                                               :status 200
                                               :body wombat-record)))))
    ch))
