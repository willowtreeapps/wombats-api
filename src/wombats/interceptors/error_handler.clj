(ns wombats.interceptors.error-handler
  (:require [io.pedestal.interceptor.error :refer [error-dispatch]]))

(defn- get-exception-data
  [exception]
  (or (-> exception
          ex-data
          :exception
          ex-data)
      ;; Handle wrapped exceptions throw by Datomic's
      ;; transactor functions
      (try
        (-> exception
            ex-data
            :exception
            .getCause
            ex-data)
        (catch Exception e
          nil))))

(defn- get-exception-type
  [exception]
  (-> exception
      get-exception-data
      :type))

(defn- resp-custom-ex
  [exception status]
  {:status status
   :body (get-exception-data exception)
   :headers {"Content-Type" "application/edn"}})

(def service-error-handler
  (error-dispatch
   [context exception]

   [{:exception-type ExceptionInfo}]
   (condp = (get-exception-type exception)
     :invalid-schema (assoc context :response (resp-custom-ex exception 400))
     :unauthorized (assoc context :response (resp-custom-ex exception 401))
     :wombat-error (assoc context :response (resp-custom-ex exception 400))
     (assoc context :io.pedestal.interceptor.chain/error exception))

   :else
   (assoc context :io.pedestal.interceptor.chain/error exception)))
