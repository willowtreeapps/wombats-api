(ns wombats.interceptors.error-handler
  (:require [io.pedestal.interceptor.error :refer [error-dispatch]]))


(defn- get-exception-data
  [exception]
  (-> exception
      ex-data
      :exception
      ex-data))

(defn- get-exception-type
  [exception]
  (-> exception
      get-exception-data
      :type))

(def service-error-handler
  (error-dispatch
   [context exception]

   [{:exception-type ExceptionInfo}]
   (condp = (get-exception-type exception)
     :invalid-schema (let [data (get-exception-data exception)]
                       (assoc context :response {:status 401
                                                 :body data
                                                 :headers {"Content-Type" "application/edn"}}))
     (assoc context :io.pedestal.interceptor.chain/error exception))

   :else
   (assoc context :io.pedestal.interceptor.chain/error exception)))
