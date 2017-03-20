(ns wombats.handlers.simulator
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [taoensso.timbre :as log]
            [clojure.spec :as s]
            [wombats.specs.utils :as sutils]
            [wombats.game.initializers :as i]
            [wombats.game.processor :refer [frame-processor]]
            [wombats.interceptors.current-user :refer [get-current-user]]
            [wombats.daos.helpers :as dao]
            [wombats.handlers.helpers :refer [wombat-error]]
            [wombats.constants :refer [initial-stats]]))

;; Swagger Parameters
(def ^:private simulator-body-params
  {:name "simulator-body"
   :in "body"
   :description "values to initialize a new simulation"
   :required true
   :default (str {:simulator/wombat-id ""
                  :simulator/template-id ""})})

(def ^:swagger-spec get-simulator-arena-templates-spec
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

(def ^:swagger-spec get-simulator-arena-template-by-id-spec
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

(s/def :simulator/wombat-id string?)
(s/def :simulator/template-id string?)

(s/def ::simulator-params (s/keys :req [:simulator/wombat-id
                                        :simulator/template-id]))

(def ^:swagger-spec initialize-simulator-spec
  {"/api/v1/simulator/initialize"
   {:post {:description "Returns the initial frame for the simulator"
           :tags ["simulator"]
           :operationId "initialize-simulator"
           :parameters [simulator-body-params]
           :responses {:200 {:description "initialize response"}}}}})

(def initialize-simulator
  "Returns the initial frame of the simulation"
  (interceptor/before
   ::initialize
   (fn [{:keys [response request] :as context}]

     (sutils/validate-input ::simulator-params (:edn-params request))

     (let [{wombat-id :simulator/wombat-id
            template-id :simulator/template-id} (:edn-params request)

           {user-id :user/id}
           (get-current-user context)

           simulator-template
           ((dao/get-fn :get-simulator-arena-template-by-id context)
            template-id)

           user
           ((dao/get-fn :get-entire-user-by-id context)
            user-id)

           wombat
           ((dao/get-fn :get-wombat-by-id context)
            wombat-id)]

       (when-not simulator-template
         (wombat-error {:code :handlers.simulator.initialize-simulator/missing-template
                        :params [template-id]}))

       (when-not user
         (wombat-error {:code :handlers.simulator.initialize-simulator/missing-user
                        :params [user-id]}))

       (when-not wombat
         (wombat-error {:code :handlers.simulator.initialize-simulator/missing-wombat
                        :params [wombat-id]}))

       (let [player-id (dao/gen-id)
             game-state {:game/arena (:simulator-template/arena-template simulator-template)
                         :game/players
                         {player-id
                          {:player/id player-id
                           :player/color "gray"
                           :player/stats initial-stats
                           :player/user {:user/github-username (:user/github-username user)
                                         :user/github-access-token (:user/github-access-token user)}
                           :player/wombat {:wombat/id (:wombat/id wombat)
                                           :wombat/name (:wombat/name wombat)
                                           :wombat/url (:wombat/url wombat)}
                           :state {:code nil
                                   :command nil
                                   :error nil
                                   :saved-state {}}}}
                         :game/frame {:frame/frame-number 0
                                      :frame/round-number 1
                                      :frame/round-start-time nil
                                      :frame/arena (:simulator-template/arena simulator-template)}}]

         (assoc context
                :response
                (assoc response
                       :status 200
                       :body (-> game-state
                                 (i/initialize-game-state)))))))))

(def ^:swagger-spec process-simulation-frame-spec
  {"/api/v1/simulator/process_frame"
   {:post {:description "Returns the next state of the simulator, processing a given frame."
           :tags ["simulator"]
           :operationId "process-simulation-frame"
           :responses {:200 {:description "process-frame response"}}}}})

(defn process-simulation-frame
  [aws-credentials lambda-settings]
  (interceptor/before
   ::process-simulation-frame
   (fn [{:keys [response request] :as context}]
     (let [game-state (:game-state (:edn-params request))]
       ;; TODO #331 Spec out game-state
       (assoc context
                :response
                (assoc response
                       :status 200
                       :body (-> game-state
                                 (frame-processor {:aws-credentials aws-credentials
                                                   :minimum-frame-time 0
                                                   :attach-mini-maps true}
                                                  lambda-settings))))))))
