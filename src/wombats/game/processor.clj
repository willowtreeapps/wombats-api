(ns wombats.game.processor
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [wombats.game.partial :refer [get-partial-arena]]
            [wombats.game.occlusion :refer [get-occluded-arena]]
            [wombats.game.utils :as gu]
            [wombats.arena.utils :as au])
  (:import [com.amazonaws.auth
            BasicAWSCredentials]
           [com.amazonaws.services.lambda
            AWSLambdaClient
            model.InvokeRequest]))
  
(defn- add-player-global-coords
  "Add the coordinates that a player is globally"
  [player-state game-state player-eid]
  (assoc player-state
         :global-coords
         (gu/get-player-coords (get-in game-state
                                       [:frame :frame/arena])
                               player-eid)))

(defn- add-player-partial-view
  "Creates a partial view for a player"
  [{:keys [global-coords] :as player-state} game-state]
  (assoc player-state
         :arena
         (get-partial-arena game-state global-coords)))

(defn- add-player-local-coords
  "Add the coordinates that a player is locally (in partial view)"
  [{:keys [arena] :as player-state} player-eid]
  (assoc player-state
         :local-coords
         (gu/get-player-coords arena player-eid)))

(defn- add-player-occlusion-view
  "Adds occlusion to a players partial view"
  [{:keys [arena local-coords] :as player-state}
   {:keys [arena-config] :as game-state}]

  (assoc player-state
         :arena
         (get-occluded-arena arena local-coords arena-config)))

(defn- add-custom-player-state
  "Adds the custom state from the previous frame"
  [player-state game-state player-eid]

  (assoc player-state
         :saved-state
         (get-in game-state [:players player-eid :saved-state])))

(defn- calculate-player-state
  [{:keys [players frame] :as game-state} player-eid]
  (let [arena (:frame/arena frame)]
    (-> {}
        (add-player-global-coords game-state player-eid)
        (add-player-partial-view game-state)
        (add-player-local-coords player-eid)
        (add-player-occlusion-view game-state)
        (add-custom-player-state game-state player-eid))))

(defn- lambda-client
  [aws-credentials]
  (let [access-key (:access-key-id aws-credentials)
        secret-key (:secret-key aws-credentials)
        credentials (new BasicAWSCredentials access-key secret-key)
        client (new AWSLambdaClient credentials)]
    client))

(defn- lambda-invoke-request
  []
  (let [request (new InvokeRequest)]
    (.setFunctionName request "arn:aws:lambda:us-east-1:356223155086:function:wombats-clojure")
    (.setPayload request (json/write-str {:arena nil
                                          :code "(fn [time-left arena] {:saved-state {:updated true} :command {:turn :right}})"}))
    request))

(defn- lambda-request
  [player-state aws-credentials]
  ;; TODO #162
  (let [client (lambda-client aws-credentials)
        request (lambda-invoke-request)
        result (.invoke client request)
        response (.getPayload result)
        response-string (new String (.array response) "UTF-8")
        object (json/read-str response-string)]

    (future object)))
    
(defn- get-lamdba-channels
  "Kicks off the AWS Lambda process of sourcing user code"
  [{:keys [players] :as game-state} aws-credentials]

  (map (fn [[player-eid {:keys [code]}]]
         (let [ch (async/chan 1)]
           (async/go
             (try
               (let [lambda-resp @(lambda-request (calculate-player-state game-state
                                                                          player-eid) 
                                                  aws-credentials)]
                 (async/>! ch {:player-eid player-eid
                               :response lambda-resp
                               :error nil}))
               (catch Exception e
                 (async/>! ch {:player-eid player-eid
                               :response nil
                               :error e}))))
           ch))
       players))

(defn source-user-decisions
  "Source users decisions by running their code through AWS Lambda"
  [game-state aws-credentials]
  (let [lambda-chans (get-lamdba-channels game-state aws-credentials)
        user-responses (async/<!! (async/map vector lambda-chans))]
    (reduce
     (fn [game-state-acc {:keys [player-eid response error]}]
       (update game-state-acc
               :players
               (fn [players]
                 (let [player
                       (get players player-eid)

                       player-update
                       (merge player {:command (get response :command nil)
                                      ;; Note: Saved state should not be updated
                                      ;;       to nil on error
                                      :saved-state (get response
                                                        :saved-state
                                                        (:saved-state player))
                                      :error error})]
                   (merge players {player-eid player-update})))))
     game-state
     user-responses)))

(defn process-user-decisions
  [game-state]
  ;; TODO #151
  game-state)
