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
