(ns wombats.handlers.arena
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.spec :as s]
            [wombats.daos.helpers :as dao]
            [wombats.specs.utils :as sutils]))

(def ^:private arena-body-sample
  #:arena{:name "Arena Name"
          :width 50
          :height 50
          :shot-damage 10
          :smoke-duration 3
          :food 10
          :poison 10
          :stone-walls 50
          :wood-walls 50
          :zakano 4
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

(defbefore get-arenas
  "Returns a seq of arena configurations"
  [{:keys [response] :as context}]
  (let [get-arenas (dao/get-fn :get-arenas context)]
    (assoc context :response (assoc response
                                    :status 200
                                    :body (get-arenas)))))

(def ^:swagger-spec get-arena-by-id-spec
  {"/api/v1/arenas/{arena-id}"
   {:get {:description "Returns an arena matching the arena-id"
          :tags ["arena"]
          :operationId "get-arena-by-id"
          :parameters [arena-id-path-param]
          :response {:200 {:description "get-arena-by-id response"}}}}})

(defbefore get-arena-by-id
  [{:keys [request response] :as context}]
  (let [get-arena (dao/get-fn :get-arena-by-id context)
        arena-id (get-in request [:path-params :arena-id])]
    (assoc context :response (assoc response
                                    :status 200
                                    :body (get-arena arena-id)))))

(def ^:swagger-spec delete-arena-spec
  {"/api/v1/arenas/{arena-id}"
   {:delete {:description "Retracts and arena configuration"
             :tags ["arena"]
             :operationId "delete-arena"
             :parameters [arena-id-path-param]
             :response {:200 {:description "delete-arena response"}}}}})

(defbefore delete-arena
  [{:keys [request response] :as context}]
  (let [retract-arena (dao/get-fn :retract-arena context)
        arena-id (get-in request [:path-params :arena-id])]

    (assoc context :response (assoc response
                                    :status 200
                                    :body @(retract-arena arena-id)))))

;; Arena Specs
(s/def :arena/id string?)
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

(s/def ::update-arena (s/merge ::new-arena
                               (s/keys :req [:arena/id])))

(def ^:swagger-spec add-arena-spec
  {"/api/v1/arenas"
   {:post {:description "Creates a new arena configuration and returns it"
           :tags ["arena"]
           :operationId "add-arena"
           :parameters [new-arena-body-params]
           :response {:200 {:description "add-arena response"}}}}})

(defbefore add-arena
  "Creates and returns an arena configuration"
  [{:keys [request response] :as context}]
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
                                      :body arena-record)))))

(def ^:swagger-spec update-arena-spec
  {"/api/v1/arenas/{arena-id}"
   {:put {:description "Updates an arena configuration"
          :tags ["arena"]
          :operationId "delete-arena"
          :parameters [arena-id-path-param
                       update-arena-body-params]
          :response {:200 {:description "update-arena response"}}}}})

(defbefore update-arena
  "Updates and returns an arena configuration"
  [{:keys [request response] :as context}]
  (let [update-arena (dao/get-fn :update-arena context)
        get-arena (dao/get-fn :get-arena-by-id context)
        arena-id (get-in request [:path-params :arena-id])
        arena (merge (:edn-params request)
                     {:arena/id arena-id})]

    (sutils/validate-input ::update-arena arena)

    @(update-arena arena)

    (assoc context :response (assoc response
                                    :status 200
                                    :body (get-arena (:arena/id arena))))))
