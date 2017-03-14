(ns wombats.handlers.access-keys
  (:require [io.pedestal.interceptor.helpers :as interceptor]))

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
     (assoc context :response (assoc response
                                     :status 200
                                     :body [])))))

(def ^:swagger-spec add-access-key-spec
  {"/api/v1/access_keys"
   {:post {:description "Creates and returns a new access key"
           :tags ["access-key"]
           :operationId "add-access-keys"
           :responses {:200 {:description "add-access-key response"}}}}})

(def get-access-keys
  (interceptor/before
   ::get-access-keys
   (fn [{:keys [response] :as context}]
     (assoc context :response (assoc response
                                     :status 200
                                     :body [])))))
