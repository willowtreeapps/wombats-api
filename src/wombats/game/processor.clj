(ns wombats.game.processor
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [wombats.game.partial :refer [get-partial-arena]]
            [wombats.game.occlusion :refer [get-occluded-arena]]
            [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]
            [wombats.game.decisions.turn :refer [turn]]
            [wombats.game.decisions.move :refer [move]]
            [wombats.game.decisions.shoot :refer [shoot]])

  (:import [com.amazonaws.auth
            BasicAWSCredentials]
           [com.amazonaws.services.lambda
            AWSLambdaClient
            model.InvokeRequest]))

(defn- add-global-coords
  "Add the global coordinates of decision maker"
  [state {:keys [frame]} uuid]
  (assoc state
         :global-coords
         (gu/get-item-coords (:frame/arena frame) uuid)))

(defn- add-local-coords
  "Add the coordinates that a decision maker is locally positioned at (in partial view)"
  [{:keys [arena] :as state} uuid]
  (assoc state
         :local-coords
         (gu/get-item-coords arena uuid)))

(defn- add-partial-view
  "Creates a partial view for a decision maker"
  [{:keys [global-coords] :as player-state}
   game-state
   decision-maker-type]
  (assoc player-state
         :arena
         (get-partial-arena game-state global-coords decision-maker-type)))

(defn- add-occlusion-view
  "Adds occlusion to a decision makers partial view"
  [{:keys [arena local-coords] :as state}
   {:keys [arena-config] :as game-state}
   decision-maker-type]

  (assoc state
         :arena
         (get-occluded-arena arena
                             local-coords
                             arena-config
                             decision-maker-type)))

(defn- add-custom-state
  "Adds the custom state from the previous frame"
  [state
   game-state
   uuid
   decision-maker-type]

  (let [decision-maker-lookup (if (= decision-maker-type :wombat) :players :zakano)
        custom-state (get-in game-state [decision-maker-lookup uuid :state :saved-state] {})]
    (assoc state :saved-state custom-state)))

(defn- add-decision-maker
  [state {:keys [frame] :as game-state} uuid]
  (assoc state :decision-maker (gu/get-item-and-coords (:frame/arena frame)
                                                       uuid)))

(defn- calculate-decision-maker-state
  [{:keys [players zakano frame] :as game-state} uuid type]
  (let [arena (:frame/arena frame)]
    (-> {}
        (add-global-coords game-state uuid)
        (add-partial-view game-state type)
        (add-local-coords uuid)
        (add-occlusion-view game-state type)
        (add-custom-state game-state uuid type))))

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
                                          :code "(fn [time-left arena] {:saved-state {:updated true} :command {:action :shoot :metadata{}}})"}))
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
  "Kicks off the AWS Lambda process"
  [{:keys [initiative-order] :as game-state} aws-credentials]
  (map (fn [{:keys [uuid type]}]
         (let [ch (async/chan 1)]
           (async/go
             (try
               (let [lambda-resp @(lambda-request (calculate-decision-maker-state game-state
                                                                                  uuid
                                                                                  type)
                                                  aws-credentials)]
                 (async/>! ch {:uuid uuid
                               :response lambda-resp
                               :error nil
                               :type type}))
               (catch Exception e
                 (async/>! ch {:uuid uuid
                               :response nil
                               :error e
                               :type type}))))
           ch))
       initiative-order))

(defn source-decisions
  "Source decisions by running their code through AWS Lambda"
  [game-state aws-credentials]
  (let [lambda-chans (get-lamdba-channels game-state aws-credentials)
        lambda-responses (async/<!! (async/map vector lambda-chans))]
    (reduce
     (fn [game-state-acc {:keys [uuid response error type]}]
       (update game-state-acc
               (if (= type :wombat) :players :zakano)
               (fn [decision-makers]
                 (let [decision-maker (get decision-makers uuid)

                       decision-maker-update
                       (assoc decision-maker :state
                              (merge (:state decision-maker)
                                     {:command (get response :command nil)
                                      ;; Note: Saved state should not be updated
                                      ;;       to nil on error
                                      :saved-state (get response
                                                        :saved-state
                                                        (:saved-state decision-maker))
                                      :error error}))]
                   (merge decision-makers {uuid decision-maker-update})))))
     game-state
     lambda-responses)))

(defn- build-decision-maker-data
  [game-state uuid]
  (-> {}
      (add-decision-maker game-state uuid)))

(def ^:private command-map
  {:turn turn
   :move move
   :shoot shoot})

(defn- get-command
  "Returns the request command handler or an identity function if none exist"
  [command]
  (get command-map command (fn [game-state _ _]
                             game-state)))

(defn- process-command
  "Process a decision makers command"
  [game-state {decision-maker-uuid :uuid
               decision-maker-type :type}]

  (let [{{action :action
          metadata :metadata}
         :command} (get-in game-state
                           [(if (= decision-maker-type :wombat)
                              :players
                              :zakano)
                            decision-maker-uuid
                            :state]
                           {})
        cmd-function (get-command action)]

    (cmd-function game-state
                  (or metadata {})
                  (build-decision-maker-data game-state
                                             decision-maker-uuid))))

(defn process-decisions
  [game-state]
  (reduce process-command game-state (:initiative-order game-state)))
