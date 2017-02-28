(ns wombats.handlers.arena
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [clojure.spec :as s]
            [wombats.daos.helpers :as dao]
            [wombats.specs.utils :as sutils]))

(def ^:private arena-body-sample
  #:arena{:name "Arena Name"
          :width 20
          :height 20
          :shot-damage 10
          :smoke-duration 3
          :food 10
          :poison 10
          :steel-walls 10
          :steel-wall-hp 500
          :wood-walls 10
          :wood-wall-hp 30
          :zakano 5
          :zakano-hp 50
          :wombat-hp 200
          :perimeter true})

;; Swagger Parameters
(def ^:private arena-id-path-param
  {:name "arena-id"
   :in "path"
   :description "id of the arena"
   :required true})

(def ^:private new-arena-body-params
  {:name "new-arena-body"
   :in "body"
   :description "values for a new arena configuration"
   :required true
   :default (str arena-body-sample)
   :schema {}})

(def ^:private update-arena-body-params
  {:name "update-arena-body"
   :in "body"
   :description "values for an updated arena configuration"
   :required true
   :default (str arena-body-sample)
   :schema {}})

;; Handlers
(def ^:swagger-spec get-arenas-spec
  {"/api/v1/arenas"
   {:get {:description "Returns a seq of all arenas"
          :tags ["arena"]
          :operationId "get-arenas"
          :responses {:200 {:description "get-arenas response"}}}}})

(def get-arenas
  "Returns a seq of arena configurations"
  (interceptor/before
   ::get-arenas
   (fn [{:keys [response] :as context}]
     (let [get-arenas (dao/get-fn :get-arenas context)]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-arenas)))))))

(def ^:swagger-spec get-arena-by-id-spec
  {"/api/v1/arenas/{arena-id}"
   {:get {:description "Returns an arena matching the arena-id"
          :tags ["arena"]
          :operationId "get-arena-by-id"
          :parameters [arena-id-path-param]
          :response {:200 {:description "get-arena-by-id response"}}}}})

(def get-arena-by-id
  "Returns a single arena when given an id"
  (interceptor/before
   ::get-arena-by-id
   (fn [{:keys [request response] :as context}]
     (let [get-arena (dao/get-fn :get-arena-by-id context)
           arena-id (get-in request [:path-params :arena-id])]
       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-arena arena-id)))))))

(def ^:swagger-spec delete-arena-spec
  {"/api/v1/arenas/{arena-id}"
   {:delete {:description "Retracts and arena configuration"
             :tags ["arena"]
             :operationId "delete-arena"
             :parameters [arena-id-path-param]
             :response {:200 {:description "delete-arena response"}}}}})

(def delete-arena
  "Removes an arena configuration"
  (interceptor/before
   ::delete-arena
   (fn [{:keys [request response] :as context}]
     (let [retract-arena (dao/get-fn :retract-arena context)
           arena-id (get-in request [:path-params :arena-id])]

       (assoc context :response (assoc response
                                       :status 200
                                       :body @(retract-arena arena-id)))))))

(def arena-constraints
  #(and (instance? Long %)
        (<= % 25)
        (>= % 5)))

;; Arena Specs
(s/def :arena/id string?)
(s/def :arena/name string?)
(s/def :arena/width arena-constraints)
(s/def :arena/height arena-constraints)
(s/def :arena/shot-damage #(instance? Long %))
(s/def :arean/smoke-duration #(instance? Long %))
(s/def :arena/food #(instance? Long %))
(s/def :arena/poison #(instance? Long %))
(s/def :arena/steel-walls #(instance? Long %))
(s/def :arena/steel-wall-hp #(instance? Long %))
(s/def :arena/wood-walls #(instance? Long %))
(s/def :arena/wood-wall-hp #(instance? Long %))
(s/def :arena/zakano #(instance? Long %))
(s/def :arena/zakano-hp #(instance? Long %))
(s/def :arena/wombat-hp #(instance? Long %))
(s/def :arena/perimeter boolean?)

(s/def ::new-arena (s/keys :req [:arena/name
                                 :arena/width
                                 :arena/height
                                 :arena/shot-damage
                                 :arena/smoke-duration
                                 :arena/food
                                 :arena/poison
                                 :arena/wombat-hp
                                 :arena/zakano
                                 :arena/zakano-hp
                                 :arena/wood-walls
                                 :arena/wood-wall-hp
                                 :arena/steel-walls
                                 :arena/steel-wall-hp]))

(s/def ::update-arena (s/merge ::new-arena
                               (s/keys :req [:arena/id])))

(def ^:swagger-spec add-arena-spec
  {"/api/v1/arenas"
   {:post {:description "Creates a new arena configuration and returns it"
           :tags ["arena"]
           :operationId "add-arena"
           :parameters [new-arena-body-params]
           :response {:200 {:description "add-arena response"}}}}})

(def add-arena
  "Creates and returns an arena configuration"
  (interceptor/before
   ::add-arena
   (fn [{:keys [request response] :as context}]
     (let [add-arena (dao/get-fn :add-arena context)
           get-arena (dao/get-fn :get-arena-by-id context)
           arena (:edn-params request)]

       (sutils/validate-input ::new-arena arena)

       (let [arena-id (dao/gen-id)
             new-arena (merge arena {:arena/id arena-id})
             tx @(add-arena new-arena)
             arena-record (get-arena arena-id)]
         (assoc context :response (assoc response
                                         :status 200
                                         :body arena-record)))))))

(def ^:swagger-spec update-arena-spec
  {"/api/v1/arenas/{arena-id}"
   {:put {:description "Updates an arena configuration"
          :tags ["arena"]
          :operationId "delete-arena"
          :parameters [arena-id-path-param
                       update-arena-body-params]
          :response {:200 {:description "update-arena response"}}}}})

(def update-arena
  "Updates and returns an arena configuration"
  (interceptor/before
   ::update-arena
   (fn [{:keys [request response] :as context}]
     (let [update-arena (dao/get-fn :update-arena context)
           get-arena (dao/get-fn :get-arena-by-id context)
           arena-id (get-in request [:path-params :arena-id])
           arena (merge (:edn-params request)
                        {:arena/id arena-id})]

       (sutils/validate-input ::update-arena arena)

       @(update-arena arena)

       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-arena (:arena/id arena))))))))
