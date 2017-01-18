(ns wombats.handlers.arena
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]
            [clojure.spec :as s]
            [wombats.daos.helpers :as dao]
            [wombats.specs.utils :as sutils]))

;; Swagger Parameters
(def ^:private arena-id-path-param
  {:name "arena id"
   :in "path"
   :description "id of the arena"
   :required true})

(def ^:private arena-body-params
  {:name "arena body"
   :in "body"
   :description "values for a new arena configuration"
   :required true
   :schema {}})

;; Handlers
(def ^{:swagger-spec true} get-arenas-spec
  {"/api/v1/arenas"
   {:get {:description "Returns a seq of all arenas"
          :tags ["arena"]
          :operationId "get-arenas"
          :responses {:200 {:description "get-arenas response"}}}}})

(defbefore get-arenas
  "Returns a seq of arena configurations"
  [{:keys [response] :as context}]
  (let [ch (chan 1)
        get-arenas (dao/get-fn :get-arenas context)]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body (get-arenas)))))
    ch))

(def ^{:swagger-spec true} add-arena-spec
  {"/api/v1/arenas"
   {:post {:description "Creates a new arena configuration and returns it"
           :tags ["arena"]
           :operationId "add-arena"
           :parameters [arena-body-params]
           :response {:200 {:description "add-arena response"}}}}})

;; Arena Specs
(s/def :arena/name string?)
(s/def :arena/width #(instance? Long %))
(s/def :arena/height #(instance? Long %))
(s/def :arena/shot-damage #(instance? Long %))
(s/def :arean/smoke-duration #(instance? Long %))
(s/def :arena/food #(instance? Long %))
(s/def :arena/poison #(instance? Long %))
(s/def :arena/stone-walls #(instance? Long %))
(s/def :arena/wood-walls #(instance? Long %))
(s/def :arena/zakano #(instance? Long %))
(s/def :arena/perimeter boolean?)

(s/def ::new-arena (s/keys :req [:arena/name
                                 :arena/width
                                 :arena/height
                                 :arena/shot-damage
                                 :arena/smoke-duration
                                 :arena/food
                                 :arena/poison]))

(defbefore add-arena
  "Returns a seq of arena configurations"
  [{:keys [request response] :as context}]
  (let [ch (chan 1)
        add-arena (dao/get-fn :add-arena context)
        get-arena-by-name (dao/get-fn :get-arena-by-name context)
        arena (:edn-params request)]

    (sutils/validate-input ::new-arena arena)

    (go
      (let [tx @(add-arena arena)
            arena-record (get-arena-by-name (:arena/name arena))]
        (>! ch (assoc context :response (assoc response
                                               :status 200
                                               :body arena-record)))))
    ch))

(def ^{:swagger-spec true} get-arena-by-id-spec
  {"/api/v1/arenas/{arena-id}"
   {:get {:description "Returns an arena matching the arena-id"
          :tags ["arena"]
          :operationId "get-arena-by-id"
          :parameters [arena-id-path-param]
          :response {:200 {:description "get-arena-by-id response"}}}}})

(defbefore get-arena-by-id
  [{:keys [request response] :as context}]
  (let [ch (chan 1)
        get-arena (dao/get-fn :get-arena-by-id context)
        arena-id (get-in request [:path-params :arena-id])]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body (get-arena arena-id)))))
    ch))

(def ^{:swagger-spec true} delete-arena-spec
  {"/api/v1/arenas/{arena-id}"
   {:delete {:description "Retracts and arena configuration"
             :tags ["arena"]
             :operationId "delete-arena"
             :parameters [arena-id-path-param]
             :response {:200 {:description "delete-arena response"}}}}})

(defbefore delete-arena
  [{:keys [request response] :as context}]
  (let [ch (chan 1)
        retract-arena (dao/get-fn :retract-arena context)
        arena-id (get-in request [:path-params :arena-id])]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body @(retract-arena arena-id)))))
    ch))
