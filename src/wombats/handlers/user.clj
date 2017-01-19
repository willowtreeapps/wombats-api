(ns wombats.handlers.user
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]
            [wombats.daos.helpers :as dao]))

;; Swagger Parameters
(def ^:private user-id-path-param
  {:name "user-id"
   :in "path"
   :description "id belonging to the user"
   :required true})

(def ^:private user-access-token-header
  {:name "Authorization"
   :in "header"
   :description "access token"
   :required false})

(def ^:private wombat-body-params
  {:name "wombat body"
   :in "body"
   :description "values for a new wombat"
   :required true
   :schema {}})

;; Handlers
(def ^:swagger-spec get-users-spec
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

(def ^:swagger-spec get-user-by-id-spec
  {"/api/v1/users/{user-id}"
   {:get {:description "Returns a user matching a given id"
          :tags ["user"]
          :operationId "get-user-by-id"
          :parameters [user-id-path-param]
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

(def ^:swagger-spec get-user-self-spec
  {"/api/v1/self"
   {:get {:description "Returns a user matching a given auth token"
          :tags ["user"]
          :operationId "get-user-self"
          :parameters [user-access-token-header]
          :responses {:200 {:description "get-user-self response"}}}}})

(defbefore get-user-self
  "Returns a user by access token"
  [{:keys [response request] :as context}]
  (let [ch (chan 1)
        get-user-by-access-token (dao/get-fn :get-user-by-access-token context)
        access-token (get-in request [:headers "authorization"])]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body (get-user-by-access-token access-token)))))
    ch))

(def ^:swagger-spec get-user-wombats-spec
  {"/api/v1/users/{user-id}/wombats"
   {:get {:description "Returns a vector of wombats that belong to a user"
          :tags ["user"]
          :operationId "get-user-wombats"
          :parameters [user-id-path-param]
          :responses {:200 {:description "get-user-wombats response"}}}}})

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

(def ^:swagger-spec add-user-wombat-spec
  {"/api/v1/users/{user-id}/wombats"
   {:post {:description "Creates and returns a wombat"
           :tags ["user"]
           :operationId "add-user-wombat"
           :parameters [user-id-path-param
                        wombat-body-params]
           :responses {:200 {:description "get-user-wombats response"}}}}})

(defbefore add-user-wombat
  "Creates a new wombat and assigns it to the user"
  [{:keys [request response] :as context}]
  (let [ch (chan 1)
        add-user-wombat (dao/get-fn :add-user-wombat context)
        get-wombat-by-name (dao/get-fn :get-wombat-by-name context)
        wombat (:edn-params request)
        user-id (get-in request [:path-params :user-id])]
    (go
      (let [tx @(add-user-wombat user-id wombat)
            wombat-record (get-wombat-by-name (:wombat/name wombat))]
        (>! ch (assoc context :response (assoc response
                                               :status 200
                                               :body wombat-record)))))
    ch))
