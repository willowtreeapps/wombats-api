(ns wombats.handlers.simulator
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [clojure.spec :as s]
            [wombats.daos.helpers :as dao]
            [wombats.handlers.helpers :refer [wombat-error]]))

(def ^:swagger-spec get-simulator-arena-templates
  {"/api/v1/simulator/templates"
   {:get {:description "Returns all the available arena templates"
          :tags ["simulator"]
          :operationId "get-simulator-arena-templates"
          :responses {:200 {:description "get-simulator-arena-templates response"}}}}})

(def get-simulator-arena-templates
  "Returns a vector of templates"
  (interceptor/before
   ::get-simulator-arena-templates
   (fn [{:keys [response] :as context}]
     (let [get-simulator-arena-templates (dao/get-fn :get-simulator-arena-templates context)]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-simulator-arena-templates)))))))

(def ^:swagger-spec get-simulator-arena-template-by-id
  {"/api/v1/simulator/templates/{template-id}"
   {:get {:description "Returns the arena template that matches the provided id"
          :tags ["simulator"]
          :operationId "get-simulator-arena-template-by-id"
          :responses {:200 {:description "get-simulator-arena-template-by-id response"}}}}})

(def get-simulator-arena-template-by-id
  "Returns the matching simulator template"
  (interceptor/before
   ::get-simulator-arena-template-by-id
   (fn [{:keys [response request] :as context}]
     (let [get-simulator-arena-template-by-id (dao/get-fn :get-simulator-arena-template-by-id context)
           simulator-id (get-in request [:path-params :template-id])]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-simulator-arena-template-by-id simulator-id)))))))
