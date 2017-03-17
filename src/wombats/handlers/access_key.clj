(ns wombats.handlers.access-key
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [clojure.spec :as s]
            [clj-time.core :as t]
            [wombats.handlers.helpers :refer [wombat-error]]
            [wombats.specs.utils :as sutils]
            [wombats.daos.helpers :as dao]
            [wombats.interceptors.current-user :refer [get-current-user]]))

(def ^:private access-key-body-sample
  #:access-key{:key "wt2017_alpha"
               :max-number-of-uses 100
               :expiration-date (str (t/plus (t/now)
                                             (t/months 1)))
               :description "Initial alpha key for WillowTree internal use only"})

(def ^:private access-key-id-path-param
  {:name "access-key-id"
   :in "path"
   :description "id belonging to an access key"
   :required true})

(def ^:private access-key-body-params
  {:name "access-key-body"
   :in "body"
   :description "values for an access key"
   :required true
   :default (str access-key-body-sample)
   :schema {}})

(def ^:swagger-spec get-access-keys-spec
  {"/api/v1/access_keys"
   {:get {:description "Returns a collection of all access keys in the system"
          :tags ["access-key"]
          :operationId "get-access-keys"
          :responses {:200 {:description "get-access-keys response"}}}}})

(def get-access-keys
  (interceptor/before
   ::get-access-keys
   (fn [{:keys [response] :as context}]
     (let [get-access-keys (dao/get-fn :get-access-keys context)]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-access-keys)))))))

(s/def :access-key/id string?)
(s/def :access-key/key string?)
(s/def :access-key/max-number-of-uses #(instance? Long %))
(s/def :access-key/number-of-uses (s/and #(instance? Long %)
                                         #(> % 0)))
(s/def :access-key/expiration-date inst?)
(s/def :access-key/description string?)
(s/def :access-key/created-by #(instance? Long %))
(s/def :access-key/updated-by #(instance? Long %))

(s/def ::new-access-key (s/keys :req [:access-key/key
                                      :access-key/max-number-of-uses
                                      :access-key/expiration-date
                                      :access-key/description]))

(s/def ::access-key (s/merge ::new-access-key
                             (s/keys :req [:access-key/id
                                           :access-key/number-of-uses
                                           :access-key/created-by
                                           :access-key/updated-by])))

(def ^:swagger-spec add-access-key-spec
  {"/api/v1/access_keys"
   {:post {:description "Creates and returns a new access key"
           :tags ["access-key"]
           :operationId "add-access-keys"
           :parameters [access-key-body-params]
           :responses {:200 {:description "add-access-key response"}}}}})

(defn- format-access-key-request
  [access-key]
  (when access-key
    (-> access-key
        (update :access-key/expiration-date #(when % (read-string (str "#inst \"" % "\"")))))))

(defn- add-access-key-fields
  [{user-ref :db/id} access-key]
  (-> access-key
      (assoc :access-key/id (dao/gen-id))
      (assoc :access-key/number-of-uses 0)
      (assoc :access-key/created-by user-ref)
      (assoc :access-key/updated-by user-ref)))

(def add-access-key
  (interceptor/before
   ::add-access-key
   (fn [{:keys [request response] :as context}]
     (let [access-key (->> (:edn-params request)
                           (format-access-key-request)
                           (sutils/validate-input ::new-access-key)
                           (add-access-key-fields (get-current-user context)))
           add-access-key (dao/get-fn :add-access-key context)
           get-access-key-by-id (dao/get-fn :get-access-key-by-id context)]

       (add-access-key access-key)

       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-access-key-by-id (:access-key/id access-key))))))))

(def ^:swagger-spec get-access-key-spec
  {"/api/v1/access_keys/{access-key-id}"
   {:get {:description "Creates and returns a new access key"
          :tags ["access-key"]
          :operationId "get-access-key"
          :parameters [access-key-id-path-param]
          :responses {:200 {:description "get-access-key response"}}}}})

(def get-access-key
  (interceptor/before
   ::get-access-key
   (fn [{:keys [request response] :as context}]
     (let [access-key-id (get-in request [:path-params :access-key-id])
           get-access-key-by-id (dao/get-fn :get-access-key-by-id context)]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-access-key-by-id access-key-id)))))))

(def ^:swagger-spec delete-access-key-spec
  {"/api/v1/access_keys/{access-key-id}"
   {:delete {:description "Removes an access key"
             :tags ["access-key"]
             :operationId "delete-access-key"
             :parameters [access-key-id-path-param]
             :responses {:200 {:description "delete-access-key response"}}}}})

(def delete-access-key
  (interceptor/before
   ::delete-access-key
   (fn [{:keys [request response] :as context}]
     (let [access-key-id (get-in request [:path-params :access-key-id])
           retract-access-key (dao/get-fn :retract-access-key context)]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (retract-access-key access-key-id)))))))

(def ^:swagger-spec update-access-key-spec
  {"/api/v1/access_keys/{access-key-id}"
   {:put {:description "Updates an access key"
          :tags ["access-key"]
          :operationId "update-access-key"
          :parameters [access-key-id-path-param
                       access-key-body-params]
          :responses {:200 {:description "update-access-key response"}}}}})

(defn- update-access-key-fields
  [{user-ref :db/id}
   current-access-key
   updated-access-key]

  (let [{:keys [:access-key/number-of-uses
                :access-key/max-number-of-uses] :as access-key}
        (merge current-access-key updated-access-key)]

    (when (> number-of-uses max-number-of-uses)
      (wombat-error {:code :handlers.access_key.update-access-key-fields/max-number-of-keys
                     :datials {:max-number-of-uses max-number-of-uses
                               :number-of-uses number-of-uses}}))

    (assoc access-key :access-key/updated-by user-ref)))

(def update-access-key
  (interceptor/before
   ::update-access-key
   (fn [{:keys [request response] :as context}]
     (let [update-access-key (dao/get-fn :update-access-key context)
           access-key-id (get-in request [:path-params :access-key-id])
           current-access-key ((dao/get-fn :get-access-key-by-id context) access-key-id)
           new-access-key (->> (:edn-params request)
                               (format-access-key-request)
                               (sutils/validate-input ::new-access-key)
                               (update-access-key-fields (get-current-user context)
                                                         current-access-key))]

       (assoc context :response (assoc response
                                       :status 200
                                       :body (update-access-key access-key-id new-access-key)))))))
