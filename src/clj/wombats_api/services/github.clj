(ns wombats-api.services.github
  (:require [wombats-api.db.core :as db]
            [org.httpkit.client :as http]
            [base64-clj.core :as b64]
            [cheshire.core :refer [parse-string]]))

(def ^:private mocked-bot-code
"(defn run
  [{:keys  [arena state bot_id hp spawn-bot?] :as step-details}]
  (let [command-options [
  [{:cmd \"SHOOT\"
    :metadata  {:direction (rand-nth  [0 1 2 3 4 5 6 7]) :hp 20}}]
  [{:cmd  \"MOVE\"
  :metadata  {:direction (rand-nth  [0 1 2 3 4 5 6 7])}}]
  ]]
  {:commands (rand-nth command-options)}))")

(defn get-repo
  [repo-name username token]
  @(http/get (str "https://api.github.com/repos/" username "/" repo-name)
            {:headers {"Accept" "application/json"
                       "Authorization" (str "token " token)}}
            (fn [{:keys [body status]}]
              (if (= 404 status)
                nil
                (parse-string body true)))))

(defn decode-bot
  [content]
  (let [base64-string (clojure.string/replace content #"\n" "")]
    (b64/decode base64-string "UTF-8")))

(defn get-bot-code
  [access-token contents-url]
  (let [{:keys [status body] :as res} @(http/get contents-url {:headers {"Authorization" (str "token " access-token)
                                                                         "Accept" "application/json"}})]
    (if (= status 200)
      (decode-bot (:content (parse-string body true)))
      nil)))

(defn get-bot
  "Returns the code a bot executes"
  [player-id repo]
  mocked-bot-code
  #_(let [{:keys [bots access-token] :as player} (db/get-player-with-token player-id)
        {:keys [contents-url] :as bot} (reduce (fn [memo bot]
                                                 (if (= (:repo bot) repo)
                                                   bot
                                                   memo)) nil bots)]
    (get-bot-code access-token contents-url)))
