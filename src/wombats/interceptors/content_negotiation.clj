(ns wombats.interceptors.content-negotiation
  (:require [clojure.data.json :as json]
            [io.pedestal.http.content-negotiation :as conneg]))

(def ^:private supported-types ["application/edn" "application/json" "text/plain"])
(def ^:private default-content-type "application/edn")

(defn- accepted-type
  [context]
  (get-in context [:request :accept :field] default-content-type))

(defn- transform-content
  [body content-type]
  (case content-type
    "text/html"        body
    "text/plain"       body
    "application/edn"  (pr-str body)
    "application/json" (json/write-str body)))

(defn- coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(def coerce-body
  {:name ::coerce-body
   :leave
   (fn [context]
     (cond-> context
       (nil? (get-in context [:response :headers "Content-Type"]))
       (update-in [:response] coerce-to (accepted-type context))))})

(def content-neg-intc (conneg/negotiate-content supported-types))
