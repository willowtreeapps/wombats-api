(ns wombats.handlers.swagger
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]
            [cheshire.core :refer [generate-string]]))

(def ^{:private true
       :doc "List of handlers to be sources for api documentation"}
  handlers
  #{"user" "game" "arena" "auth"})

(def ^:private swagger-specs {:info {:title "Wombats API"
                                     :description "API documentation for Wombats"
                                     :version "1.0.0-alpha"}
                              :securityDefinitions {:Bearer {:type "apiKey"
                                                             :name "Authorization"
                                                             :in "header"}}
                              :basePath ""
                              :schemas ["http"]
                              :host ""
                              :swagger "2.0"
                              :consumes ["application/edn"]
                              :produces ["application/edn"]
                              :tags [{:name "user"
                                      :description "User API"}
                                     {:name "auth"
                                      :description "Authentication API"}
                                     {:name "arena"
                                      :description "Arena Configuration API"}
                                     {:name "game"
                                      :description "Game API"}]})

(defn- source-handler-vars
  "sources all public vars inside of each ns passed in the handlers set"
  [handlers]
  (->> handlers
       (map #(vals (ns-publics (symbol (str "wombats.handlers." %)))))
       flatten))

(defn- filter-spec-vars
  "Filters out vars that do not contain the :swagger-spec metadata key"
  [handler-vars]
  (filter (fn [var]
            (:swagger-spec (meta var)))
          handler-vars))

(defn- reduce-path-documentation
  "Reduces over spec vars producing a single path document"
  [handler-vars]
  (reduce #(merge-with merge %1 (var-get %2)) {} handler-vars))

(defn- resolve-swagger-paths
  "Returns a map that is compatible with the OpenAPI path specification
  https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#paths-object"
  []
  (-> handlers
      source-handler-vars
      filter-spec-vars
      reduce-path-documentation))

(defbefore get-specs
  [{:keys [response] :as context}]
  (let [ch (chan 1)]
    (go
      (let [specs (assoc swagger-specs :paths (resolve-swagger-paths))]
        (>! ch (assoc context :response (assoc response
                                               :status 201
                                               :body (generate-string specs)
                                               :headers {"Content-Type" "application/json"})))))
    ch))
