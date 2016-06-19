(ns battlebots.controllers.players
  (:require [ring.util.response :refer [response status]]
            [org.httpkit.client :as http]
            [battlebots.schemas.player :refer [is-bot?]]
            [battlebots.services.mongodb :as db]
            [cheshire.core :refer [parse-string]]
            [monger.result :as mr]))

(defn get-players
  "returns all players or a specified player"
  ([]
   (response (db/get-all-players)))
  ([player-id]
   (response (db/get-player player-id))))

(defn remove-player
  "removes a specified player"
  [player-id]
  (db/remove-player player-id)
  (response "ok"))

(defn get-repo
  [repo-name username token]
  (http/get (str "https://api.github.com/repos/" username "/" repo-name)
            {:headers {"Accept" "application/json"
                       "Authorization" (str "token " token)}}
            (fn [{:keys [body status]}]
              (if (= 404 status)
                nil
                (parse-string body true)))))

(defn add-player-bot
  [player-id bot]
  (is-bot? bot)
  (let [{:keys [bots access-token login] :as player} (db/get-player-with-token player-id)
        repo @(get-repo (:repo bot) login access-token)
        bot-registered? (contains? (set bots) bot)
        content-url (if repo (clojure.string/replace (:contents_url repo) #"\{\+path\}" "bot.clj"))
        update (assoc bot :contents-url content-url)]
    (if (and (not bot-registered?) repo)
      (if (mr/acknowledged? (db/add-player-bot player-id update))
        (response (db/get-player player-id)))
      (status (response "Failed to register bot") 400))))

(defn remove-player-bot
  [player-id repo]
  (if (mr/acknowledged? (db/remove-player-bot player-id repo))
    (response (db/get-player player-id))
    (status (response "Failed to remove bot" 400))))
