(ns wombats.game.processor
  (:require [cheshire.core :as cheshire]
            [clojure.core.async :as async]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [wombats.game.partial :refer [get-partial-arena]]
            [wombats.game.occlusion :refer [get-occluded-arena]]
            [wombats.game.utils :as gu]
            [wombats.arena.utils :as au]
            [wombats.game.initializers :as i]
            [wombats.game.finalizers :as f]
            [wombats.game.decisions.turn :refer [turn]]
            [wombats.game.decisions.move :refer [move]]
            [wombats.game.decisions.shoot :refer [shoot]]
            [wombats.game.decisions.smoke :refer [smoke]])
  (:import [com.amazonaws.auth
            BasicAWSCredentials]
           [com.amazonaws.services.lambda
            AWSLambdaClient
            model.InvokeRequest]))

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
  [game-state uuid type]
  (let [global-coords (gu/get-item-coords (get-in game-state [:frame :frame/arena]) uuid)]
    (if global-coords
      (-> {:global-coords global-coords}
          (add-partial-view game-state type)
          (add-local-coords uuid)
          (add-occlusion-view game-state type)
          (add-custom-state game-state uuid type))
      game-state)))

(defn- lambda-client
  [{:keys [access-key-id secret-key]}]
  (let [credentials (new BasicAWSCredentials access-key-id secret-key)]
    (new AWSLambdaClient credentials)))

(defn- lambda-request-body
  [player-state bot-code]
  (cheshire/generate-string {:code (:code bot-code)
                             :state player-state}))

(defn- lambda-invoke-request
  [player-state {:keys [path] :as bot-code} lambda-settings]
  (let [path-ext (keyword (last (clojure.string/split path #"\.")))
        lambda-uri (get lambda-settings path-ext)
        request (new InvokeRequest)
        payload (lambda-request-body player-state bot-code)]

    (.setFunctionName request lambda-uri)
    (.setPayload request payload)
    request))

(defn- lambda-request
  [decision-maker-state
   {:keys [code path]}
   aws-credentials
   lambda-settings]

  (let [client (lambda-client aws-credentials)
        request (lambda-invoke-request decision-maker-state
                                       {:code code
                                        :path path}
                                       lambda-settings)
        result (.invoke client request)
        response (.getPayload result)
        response-string (new String (.array response) "UTF-8")
        response-parsed (cheshire/parse-string response-string true)]

    (future response-parsed)))

(defn- get-decision-maker-code
  [game-state uuid type]
  (let [key-name (if (= type :wombat) :players type)]
    (get-in game-state [key-name uuid :state :code])))

(defn- get-lambda-channels
  "Kicks off the AWS Lambda process"
  [{:keys [initiative-order] :as game-state}
   aws-credentials
   lambda-settings]
  (map (fn [{:keys [uuid type]}]
         (let [ch (async/chan 1)]
           (async/go
             (try
               (let [lambda-resp @(lambda-request (calculate-decision-maker-state game-state
                                                                                  uuid
                                                                                  type)
                                                  (get-decision-maker-code game-state
                                                                           uuid
                                                                           type)
                                                  aws-credentials
                                                  lambda-settings)]

                 (async/>! ch {:uuid uuid
                               :response lambda-resp
                               :channel-error nil
                               :type type}))
               (catch Exception e
                 (async/>! ch {:uuid uuid
                               :response {}
                               :channel-error e
                               :type type}))))
           ch))
       initiative-order))

(defn source-decisions
  "Source decisions by running their code through AWS Lambda"
  [game-state
   {:keys [aws-credentials
           minimum-frame-time]}
   lambda-settings]
  (let [end-time (t/plus (t/now) (t/millis minimum-frame-time))
        lambda-chans (get-lambda-channels game-state
                                          aws-credentials
                                          lambda-settings)
        lambda-responses (async/<!! (async/map vector lambda-chans))]

    ;; If the minimum amount of time has not elapsed we want
    ;; to wait the remaining time to keep frames consistent
    (when (t/before? (t/now) end-time)
      (let [time-remaining
            (- (c/to-long end-time)
               (c/to-long (t/now)))]
        (Thread/sleep time-remaining)))

    (reduce
     (fn [game-state-acc {:keys [uuid response channel-error type]}]
       (update game-state-acc
               (if (= type :wombat) :players :zakano)
               (fn [decision-makers]
                 (let [decision-maker
                       (get decision-makers uuid)

                       {{response-command :command
                         response-state :state} :response
                        user-code-stacktrace :error
                        lambda-error :errorMessage}
                       response

                       decision-maker-update
                       (assoc decision-maker :state
                              (-> {}
                                  (merge (:state decision-maker))
                                  (merge {;; Note: Saved state should not be updated
                                          ;;       to nil on error
                                          :saved-state (or response-state
                                                           (:saved-state decision-maker))
                                          :error user-code-stacktrace
                                          :command response-command})))]
                   (merge decision-makers {uuid decision-maker-update})))))
     game-state
     lambda-responses)))

(defn- build-decision-maker-state
  [game-state uuid]
  (-> {}
      (add-decision-maker game-state uuid)))

(defn- remove-from-initiative-order
  [game-state uuid]
  (update game-state
          :initiative-order
          (fn [initiative-order]
            (filter (fn [{active-uuid :uuid}]
                      (not= active-uuid uuid))
                    initiative-order))))

(def ^:private command-map
  {:turn turn
   :move move
   :shoot shoot
   :smoke smoke})

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
        cmd-function (get-command (keyword action))
        decision-maker-state (build-decision-maker-state game-state
                                                         decision-maker-uuid)]

    (if-not (:decision-maker decision-maker-state)
      (remove-from-initiative-order game-state
                                    decision-maker-uuid)
      (cmd-function game-state
                    (or metadata {})
                    decision-maker-state))))

(defn process-decisions
  [game-state]
  (reduce process-command
          game-state
          (:initiative-order game-state)))

(defn frame-processor
  [game-state frame-processor-settings lambda-settings]
  (-> game-state
      (i/initialize-frame)
      (source-decisions frame-processor-settings lambda-settings)
      (process-decisions)
      (f/finalize-frame frame-processor-settings
                        calculate-decision-maker-state)))
