(ns wombats.handlers.game
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [clojure.spec :as s]
            [wombats.constants :refer [max-players]]
            [clj-time.core :as t]
            [wombats.interceptors.current-user :refer [get-current-user]]
            [wombats.handlers.helpers :refer [wombat-error]]
            [wombats.daos.helpers :as dao]
            [wombats.specs.utils :as sutils]
            [wombats.arena.core :refer [generate-arena]]
            [wombats.scheduler.core :as scheduler]))

(def ^:private game-body-sample
  #:game{:name "New Game"
         :max-players 8
         :type :high-score
         :round-intermission (* 1000 60 20)
         :round-length (* 1000 60)
         :num-rounds 3
         :is-private false
         :password ""
         :start-time (str (t/now))})

(def ^:private join-game-body-sample
  (merge #:player{:wombat-id ""
                  :color "#000000"}
         #:game{:password ""}))

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

(def ^:private game-status-query-param
  {:name "status"
   :in "query"
   :description "The status of the games being queried"
   :enum #{:pending-open :pending-closed :active :closed}
   :required false})

(def ^:private game-user-query-param
  {:name "user"
   :in "query"
   :description "The id of the player in a game"
   :required false})

(def ^:private game-body-params
  {:name "game-body"
   :in "body"
   :description "values for a new game"
   :required true
   :default (str game-body-sample)
   :schema {}})

(def ^:private join-game-body-params
  {:name "join-body"
   :in "body"
   :description "Body params for joining a game"
   :required true
   :default (str join-game-body-sample)
   :schema {}})

;; Handlers
(def ^:swagger-spec get-games-spec
  {"/api/v1/games"
   {:get {:description "Returns all games"
          :tags ["game"]
          :operationId "get-games"
          :parameters [game-status-query-param
                       game-user-query-param]
          :responses {:200 {:description "get-games response"}}}}})

(def get-games
  (interceptor/before
   ::get-games
   (fn [{:keys [request response] :as context}]
     (let [get-all-games (dao/get-fn :get-all-games context)
           get-games-by-status (dao/get-fn :get-game-eids-by-status context)
           get-games-by-player (dao/get-fn :get-game-eids-by-player context)
           get-games-by-eids (dao/get-fn :get-games-by-eids context)
           {status :status
            user :user} (get request :query-params {})
           games (if (empty? (:query-params request))
                   (get-all-games)
                   (get-games-by-eids (->> (cond-> []
                                             ((complement nil?) status)
                                             (conj (set (get-games-by-status status)))

                                             ((complement nil?) user)
                                             (conj (set (get-games-by-player user))))
                                           (apply clojure.set/intersection)
                                           (into []))))]
       (assoc context :response (assoc response
                                       :status 200
                                       :body games))))))

(def ^:swagger-spec get-game-by-id-spec
  {"/api/v1/games/{game-id}"
   {:get {:description "Returns a game matching the provided game id"
          :tags ["game"]
          :operationId "get-game-by-id"
          :parameters [game-id-path-param]
          :response {:200 {:description "get-game-by-id response"}}}}})

(def get-game-by-id
  (interceptor/before
   ::get-game-by-id
   (fn [{:keys [request response] :as context}]
     (let [get-game (dao/get-fn :get-game-by-id context)
           game-id (get-in request [:path-params :game-id])
           game (get-game game-id)]
       (assoc context :response (assoc response
                                       :status (if game 200 404)
                                       :body game))))))

(s/def :game/id string?)
(s/def :game/arena #(instance? Long %))
(s/def :game/status #{:open :full :in-progress :closed})
#_(s/def :game/frame) ;; Add frame specs
#_(s/def :game/players) ;; Add player specs

(s/def :game/name string?)
(s/def :game/max-players #(and (instance? Long %)
                               (not= 0 %)
                               (<= % max-players)))
(s/def :game/status #{:pending-open
                      :pending-closed
                      :active
                      :active-intermission
                      :closed})

(s/def :game/type #{:high-score})
(s/def :game/num-rounds #(instance? Long %))
(s/def :game/round-intermission #(instance? Long %))
(s/def :game/round-length #(instance? Long %))
(s/def :game/is-private boolean?)
(s/def :game/password string?)
(s/def :game/start-time inst?)

(s/def ::new-game-input (s/keys :req [:game/name
                                      :game/max-players
                                      :game/type
                                      :game/is-private
                                      :game/start-time
                                      :game/password
                                      :game/num-rounds
                                      :game/round-intermission
                                      :game/round-length]))

(s/def :player/wombat-id string?)
(s/def :player/color string?)

(s/def ::join-game-input (s/keys :req [:player/wombat-id
                                       :player/color]
                                 :opt [:game/password]))

(def ^:swagger-spec add-game-spec
  {"/api/v1/games"
   {:post {:description "Creates and returns a game"
           :tags ["game"]
           :operationId "add-game"
           :parameters [arena-id-query-param
                        game-body-params]
           :responses {:200 {:description "add-game response"}}}}})

(defn- set-game-defaults
  [{:keys [:game/password
           :game/num-rounds
           :game/round-intermission
           :game/start-time] :as game}]
  (merge game
         {:game/password (or password "")
          :game/num-rounds (or num-rounds 1)
          :game/round-intermission (or round-intermission 0)
          :game/status :pending-open
          :game/start-time (read-string (str "#inst \"" start-time "\""))}))

(def add-game
  (interceptor/before
   ::add-game
   (fn [{:keys [request response] :as context}]
     (let [add-game (dao/get-fn :add-game context)
           get-game (dao/get-fn :get-game-by-id context)
           get-arena (dao/get-fn :get-arena-by-id context)
           start-game-fn (dao/get-fn :start-game context)
           arena-id (get-in request [:query-params :arena-id])
           game (set-game-defaults (:edn-params request))
           arena-config (get-arena arena-id)]

       (when-not arena-config
         (wombat-error {:code 000000
                        :details {:arena-id arena-id}}))

       (sutils/validate-input ::new-game-input game)

       (let [game-id (dao/gen-id)
             new-game (merge game {:game/id game-id})
             game-arena (generate-arena arena-config)
             tx @(add-game new-game
                           (:db/id arena-config)
                           game-arena)
             game-record (get-game game-id)]

         (scheduler/schedule-game game-id
                                  (:game/start-time game-record)
                                  start-game-fn)

         (assoc context :response (assoc response
                                         :status 201
                                         :body game-record)))))))

(def ^:swagger-spec delete-game-spec
  {"/api/v1/games/{game-id}"
   {:delete {:description "Deletes a game"
             :tags ["game"]
             :operationId "delete-game"
             :parameters [game-id-path-param]
             :responses {:200 {:description "delete-game response"}}}}})

(def delete-game
  (interceptor/before
   ::delete-game
   (fn [{:keys [request response] :as context}]
     (let [retract-game (dao/get-fn :retract-game context)
           game-id (get-in request [:path-params :game-id])]

       (assoc context :response (assoc response
                                       :status 200
                                       :body @(retract-game game-id)))))))

(def ^:swagger-spec join-game-spec
  {"/api/v1/games/{game-id}/join"
   {:put {:description "Attaches the requesting user to an open game"
          :tags ["game"]
          :operationId "join-game"
          :parameters [game-id-path-param
                       join-game-body-params]
          :responses {:200 {:description "join-game response"}}}}})

(defn- password-match?
  "Determines if a given password matches the password for the requested game."
  [{game-password :game/password
    is-private :game/is-private}
   {user-password-attempt :game/password}]
  (if is-private
    (= game-password user-password-attempt)
    true))

(def join-game
  (interceptor/before
   ::join-game
   (fn [{:keys [request response] :as context}]
     (let [add-player-to-game (dao/get-fn :add-player-to-game context)
           get-wombat-by-id (dao/get-fn :get-wombat-by-id context)
           get-game-by-id (dao/get-fn :get-game-by-id context)
           game-id (get-in request [:path-params :game-id])
           join-params (:edn-params request)
           current-user (get-current-user context)]

       (sutils/validate-input ::join-game-input join-params)

       (let [{wombat-id :player/wombat-id
              color :player/color} join-params
             user-eid (:db/id current-user)
             wombat (get-wombat-by-id wombat-id)
             {wombat-eid :db/id} wombat
             game (get-game-by-id game-id)]

         (when-not (password-match? game join-params)
           (wombat-error {:code 000005
                          :field-error :password}))

         (when-not game
           (wombat-error {:code 000003
                          :details {:game-id game-id}}))

         (when-not wombat
           (wombat-error {:code 000001
                          :details {:wombat-id wombat-id
                                    :wombat-eid wombat-eid}}))

         (when-not (= (:wombat/owner wombat)
                      {:db/id user-eid})
           (wombat-error {:code 000002
                          :details {:user-eid user-eid
                                    :wombat-eid wombat-eid}}))

         (add-player-to-game game user-eid wombat-eid color)

         (assoc context :response (assoc response
                                         :status 200
                                         :body (get-game-by-id game-id))))))))

(def ^:swagger-spec start-game-spec
  {"/api/v1/games/{game-id}/start"
   {:put {:description "Starts the game"
          :tags ["game"]
          :operationId "start-game"
          :parameters [game-id-path-param]
          :responses {:200 {:description "start-game response"}}}}})

(defn- game-can-be-started?
  "Checks to see if a game can transition to the :active state."
  [{:keys [:game/status]}]
  (contains? #{:pending-open
               :pending-closed}
             status))

(def start-game
  (interceptor/before
   ::start-game
   (fn [{:keys [request response] :as context}]
     (let [game-id (get-in request [:path-params :game-id])
           get-game-by-id (dao/get-fn :get-game-by-id context)
           start-game-fn (dao/get-fn :start-game context)
           game (get-game-by-id game-id)]

       (when-not game
         (wombat-error {:code 000003
                        :details {:game-id game-id}}))

       (when-not (game-can-be-started? game)
         (wombat-error {:code 000004
                        :details {:game-id game-id
                                  :game-state (:game/status game)}}))

       (start-game-fn game)

       (assoc context :response (assoc response
                                       :status 200
                                       :body (get-game-by-id game-id)))))))
