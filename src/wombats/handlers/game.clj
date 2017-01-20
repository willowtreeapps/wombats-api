(ns wombats.handlers.game
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.spec :as s]
            [wombats.daos.helpers :as dao]
            [wombats.specs.utils :as sutils]))

(def ^:private game-body-sample
  #:game{:name "New Game"
         :max-players 10
         :type :round
         :round-intermission (* 1000 60 20)
         :num-rounds 3
         :is-private false})

;; Swagger Parameters
(def ^:private game-id-path-param
  {:name "game-id"
   :in "path"
   :description "id belonging to a game"
   :required true})

(def ^:private arena-id-query-param
  {:name "arena-id"
   :in "query"
   :description "id of arean"
   :required true})

(def ^:private game-body-params
  {:name "game-body"
   :in "body"
   :description "values for a new game"
   :required true
   :default (str game-body-sample)
   :schema {}})

;; Handlers
(def ^:swagger-spec get-games-spec
  {"/api/v1/games"
   {:get {:description "Returns all games"
          :tags ["game"]
          :operationId "get-games"
          :responses {:200 {:description "get-games response"}}}}})

(defbefore get-games
  [{:keys [response] :as context}]
  (let [get-games (dao/get-fn :get-games context)]
    (assoc context :response (assoc response
                                    :status 200
                                    :body (get-games)))))

(def ^:swagger-spec get-game-by-id-spec
  {"/api/v1/games/{game-id}"
   {:get {:description "Returns a game matching the provided game id"
          :tags ["game"]
          :operationId "get-game-by-id"
          :parameters [game-id-path-param]
          :response {:200 {:description "get-game-by-id response"}}}}})

(defbefore get-game-by-id
  [{:keys [request response] :as context}]
  (let [get-game (dao/get-fn :get-game-by-id context)
        game-id (get-in request [:path-params :game-id])
        game (get-game game-id)]
    (assoc context :response (assoc response
                                    :status (if game 200 404)
                                    :body game))))

(s/def :game/id string?)
(s/def :game/arena #(instance? Long %))
(s/def :game/status #{:open :full :in-progress :closed})
#_(s/def :game/frame) ;; Add frame specs
#_(s/def :game/players) ;; Add player specs

(s/def :game/name string?)
(s/def :game/max-players #(instance? Long %))
(s/def :game/type #{:round})
(s/def :game/num-rounds #(instance? Long %))
(s/def :game/round-intermission #(instance? Long %))
(s/def :game/is-private boolean?)
(s/def :game/password string?)

(s/def ::new-game-input (s/keys :req [:game/name
                                      :game/max-players
                                      :game/type
                                      :game/is-private]
                                :opt [:game/password
                                      :game/num-rounds
                                      :game/round-intermission]))

(def ^:swagger-spec add-game-spec
  {"/api/v1/games"
   {:post {:description "Creates and returns a game"
           :tags ["game"]
           :operationId "add-game"
           :parameters [arena-id-query-param
                        game-body-params]
           :responses {:200 {:description "add-game response"}}}}})

(defbefore add-game
  [{:keys [request response] :as context}]
  (let [add-game (dao/get-fn :add-game context)
        get-game (dao/get-fn :get-game-by-id context)
        arena-id (get-in request [:query-params :arena-id])
        game (:edn-params request)]

    (sutils/validate-input ::new-game-input game)

    (let [game-id (dao/gen-id)
          new-game (merge game {:game/id game-id})
          tx @(add-game new-game arena-id)
          game-record (get-game game-id)]

      (assoc context :response (assoc response
                                      :status 201
                                      :body game-record)))))

#_(def ^:swagger-spec update-game-spec
  {"/api/v1/games/{game-id}"
   {:put {:description "Updates and returns a game"
           :tags ["game"]
           :operationId "update-game"
           :parameters [game-id-path-param
                        game-body-params]
           :responses {:200 {:description "update-game response"}}}}})

(def ^:swagger-spec delete-game-spec
  {"/api/v1/games/{game-id}"
   {:delete {:description "Deletes a game"
             :tags ["game"]
             :operationId "delete-game"
             :parameters [game-id-path-param]
             :responses {:200 {:description "delete-game response"}}}}})

(defbefore delete-game
  [{:keys [request response] :as context}]
  (let [retract-game (dao/get-fn :retract-game context)
        game-id (get-in request [:path-params :game-id])]

    (assoc context :response (assoc response
                                    :status 200
                                    :body @(retract-game game-id)))))
