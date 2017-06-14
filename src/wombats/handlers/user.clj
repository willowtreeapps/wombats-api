(ns wombats.handlers.user
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [org.httpkit.client :as http]
            [clojure.spec :as s]
            [cheshire.core :as cheshire]
            [wombats.daos.helpers :as dao]
            [wombats.handlers.helpers :refer [wombat-error]]
            [wombats.interceptors.authorization :refer [authorization-error]]
            [wombats.specs.utils :as sutils]
            [wombats.constants :refer [github-repo-api-base
                                       github-repositories-by-id]]))

(def ^:private wombat-body-sample
  #:wombat{:name "Teddy"
           :url "/oconn/wombat-bots/contents/bot-one.clj"})

;; Swagger Parameters
(def ^:private user-id-path-param
  {:name "user-id"
   :in "path"
   :description "id belonging to the user"
   :required true})

(def ^:private wombat-id-path-param
  {:name "wombat-id"
   :in "path"
   :description "id belonging to the wombat"
   :required true})

(def ^:private wombat-body-params
  {:name "wombat-body"
   :in "body"
   :description "values for a new wombat"
   :required true
   :default (str wombat-body-sample)
   :schema {}})

;; Handlers
(def ^:swagger-spec get-users-spec
  {"/api/v1/users"
   {:get {:description "Returns all users"
          :tags ["user"]
          :operationId "get-users"
          :responses {:200 {:description "get-users response"}}}}})

(def get-users
  "Returns a seq of users"
  (interceptor/before
   ::get-users
   (fn [{:keys [response] :as context}]
     (let [get-users (dao/get-fn :get-users context)]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-users)))))))

(def ^:swagger-spec get-user-by-id-spec
  {"/api/v1/users/{user-id}"
   {:get {:description "Returns a user matching a given id"
          :tags ["user"]
          :operationId "get-user-by-id"
          :parameters [user-id-path-param]
          :responses {:200 {:description "get-user-by-id response"}}}}})

(def get-user-by-id
  "Returns a user by searching for its id"
  (interceptor/before
   ::get-user-by-id
   (fn [{:keys [response request] :as context}]
     (let [get-user-by-id (dao/get-fn :get-user-by-id context)
           user-id (get-in request [:path-params :user-id])]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-user-by-id user-id)))))))

(def ^:swagger-spec get-user-self-spec
  {"/api/v1/self"
   {:get {:description "Returns a user matching a given auth token"
          :tags ["user"]
          :operationId "get-user-self"
          :parameters []
          :responses {:200 {:description "get-user-self response"}}}}})

(def get-user-self
  "Returns a user by access token"
  (interceptor/before
   ::get-user-self
   (fn [{:keys [response request] :as context}]
     (let [get-user-by-access-token (dao/get-fn :get-user-by-access-token context)
           access-token (get-in request [:headers "authorization"])
           user (get-user-by-access-token access-token)]
       (assoc context :response (assoc response
                                       :status (if user 200 401)
                                       :body user))))))

(def ^:swagger-spec get-user-wombats-spec
  {"/api/v1/users/{user-id}/wombats"
   {:get {:description "Returns a vector of wombats that belong to a user"
          :tags ["user"]
          :operationId "get-user-wombats"
          :parameters [user-id-path-param]
          :responses {:200 {:description "get-user-wombats response"}}}}})

(def get-user-wombats
  "Returns a seq of user wombats"
  (interceptor/before
   ::get-user-wombats
   (fn [{:keys [response request] :as context}]
     (let [get-user-wombats (dao/get-fn :get-user-wombats context)
           user-id (get-in request [:path-params :user-id])]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-user-wombats user-id)))))))

(s/def :wombat/id string?)
(s/def :wombat/name string?)
(s/def :wombat/url string?)

(s/def ::wombat-params (s/keys :req [:wombat/name
                                     :wombat/url]
                               :opt [:wombat/id]))

(def ^:swagger-spec add-user-wombat-spec
  {"/api/v1/users/{user-id}/wombats"
   {:post {:description "Creates and returns a wombat"
           :tags ["user"]
           :operationId "add-user-wombat"
           :parameters [user-id-path-param
                        wombat-body-params]
           :responses {:200 {:description "get-user-wombats response"}}}}})

(def add-user-wombat
  "Creates a new wombat and assigns it to the user"
  (interceptor/before
   ::add-user-wombat
   (fn [{:keys [request response] :as context}]
     (let [add-user-wombat (dao/get-fn :add-user-wombat context)
           get-entire-user-by-id (dao/get-fn :get-entire-user-by-id context)
           get-wombat (dao/get-fn :get-wombat-by-id context)
           wombat (:edn-params request)
           user-id (get-in request [:path-params :user-id])
           wombat-id (dao/gen-id)
           new-wombat (merge wombat {:wombat/id wombat-id})
           user (get-entire-user-by-id user-id)]

       (when-not user
         (wombat-error {:code 001000
                        :details {:user-id user-id}}))

       (sutils/validate-input ::wombat-params wombat)

       (let [url (str github-repo-api-base (:wombat/url new-wombat))
             auth-headers {:headers {"Accept" "application/json"
                                     "Authorization" (str "token "
                                                          (:user/github-access-token user))}}
             {status :status} @(http/get url auth-headers)]

         (if (= status 200)
           (do
             @(add-user-wombat (:db/id user) new-wombat)
             (assoc context :response (assoc response
                                             :status 200
                                             :body (get-wombat wombat-id))))
           (wombat-error {:code 001001
                          :params [(:wombat/name new-wombat)
                                   (:wombat/url new-wombat)]})))))))

(def ^:swagger-spec get-user-repositories-spec
  {"/api/vi/users/{user-id}/repositories"
   {:get {:description "Returns a vector of repositories that belong to a user"
          :tags ["user"]
          :operationId "get-user-repositories"
          :parameters []
          :responses {:200 {:description "get-user-repositories response"}}}}})

(defn- filter-hashmap-fields
  "Function to remove fields from Github's API return.
  Fields is a vector of keys and hashmap is the parsed map"
  [hashmap fields]
  (map #(select-keys % fields) hashmap))

(def get-user-repositories
  "Return a list of the users repositories"
  (interceptor/before
     ::get-user-repositories
     (fn [{:keys [response request] :as context}]

       (let [get-entire-user-by-id (dao/get-fn :get-entire-user-by-id context)
             user-id (get-in request [:path-params :user-id])
             user (get-entire-user-by-id user-id)]
         (when-not user
           (wombat-error {:code 001000
                          :details {:user-id user-id}}))
         (let [url (github-repositories-by-id (:user/github-username user))
               auth-headers {:headers {"Accept" "application/json"}}
               {:keys [status headers body error] :as resp} @(http/get url auth-headers)
               repository-names (filter-hashmap-fields (cheshire/parse-string body true) [:name :updated_at :description :url] )]
           (assoc context :response (assoc response
                                           :status status
                                           :headers {"Content-Type" "application/edn"}
                                           :body repository-names)))))))

(defn- user-owns-wombat?
  "Determines if a user owns a wombat"
  [user-id wombat-id context]
  (let [get-owner-id (dao/get-fn :get-wombat-owner-id context)
        owner-id (get-owner-id wombat-id)]
    (= user-id owner-id)))

(def ^:swagger-spec delete-wombat-spec
  {"/api/v1/users/{user-id}/wombats/{wombat-id}"
   {:delete {:description "Deletes a users wombat"
             :tags ["user"]
             :operationId "delete-wombat"
             :parameters [user-id-path-param
                          wombat-id-path-param]
             :responses {:200 {:description "delete-wombat response"}}}}})

(def delete-wombat
  (interceptor/before
   ::delete-wombat
   (fn [{:keys [request response] :as context}]
     (let [retract-wombat (dao/get-fn :retract-wombat context)
           {wombat-id :wombat-id
            user-id :user-id} (:path-params request)]

       (when-not (user-owns-wombat? user-id wombat-id context)
         (authorization-error "Cannot remove this wombat"))

       (assoc context :response (assoc response
                                       :status 200
                                       :body @(retract-wombat wombat-id)))))))

(def ^:swagger-spec update-wombat-spec
  {"/api/v1/users/{user-id}/wombats/{wombat-id}"
   {:put {:description "Updates a users wombat"
          :tags ["user"]
          :operationId "update-wombat"
          :parameters [user-id-path-param
                       wombat-id-path-param
                       wombat-body-params]
          :responses {:200 {:description "update-wombat response"}}}}})

(def update-wombat
  (interceptor/before
   ::update-wombat
   (fn [{:keys [request response] :as context}]
     (let [update-wombat (dao/get-fn :update-user-wombat context)
           get-wombat (dao/get-fn :get-wombat-by-id context)
           get-entire-user-by-id (dao/get-fn :get-entire-user-by-id context)
           {wombat-id :wombat-id
            user-id :user-id} (:path-params request)
           wombat (merge (:edn-params request)
                         {:wombat/id wombat-id})
           user (get-entire-user-by-id user-id)]

       (sutils/validate-input ::wombat-params wombat)

       (when-not user
         (wombat-error {:code 001000
                        :details {:user-id user-id}}))

       (when-not (user-owns-wombat? user-id wombat-id context)
         (authorization-error "Cannot update this wombat"))

       (let [url (str github-repo-api-base (:wombat/url wombat))
             auth-headers {:headers {"Accept" "application/json"
                                     "Authorization" (str "token "
                                                          (:user/github-access-token user))}}
             {status :status} @(http/get url auth-headers)]

         (if (= status 200)
           (do
             @(update-wombat wombat)
             (assoc context :response (assoc response
                                             :status 200
                                             :body (get-wombat wombat-id))))
           (wombat-error {:code 001001
                          :params [(:wombat/name wombat)
                                   (:wombat/url wombat)]})))))))
