(ns wombats.interceptors.error-logger
  (:require [taoensso.timbre :as log]))

(def error-logger
  {:name ::report-errors
   :error (fn [context error]
            (log/error (ex-data error))
            context)})
