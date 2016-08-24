(ns wombats-api.controllers.player
  (:require [ring.util.http-response :refer :all]
            [cheshire.core :refer [parse-string]]
            [wombats-api.db.core :as db]
            [wombats-api.services.github :refer [get-repo]]))

(defn get-players
  ([]
   (ok (db/get-all-players)))
  ([player-id]
   (ok (db/get-player player-id))))

(defn remove-player
  [player-id]
  (db/remove-player player-id)
  (ok (str "Removed player.")))

(defn add-player-bot
  [{:keys [bots access-token login _id] :as player}
   {:keys [repo name] :as bot}]
  (let [gh-repo (get-repo repo login access-token)
        bot-registered? (contains? (set (map #(:repo %) bots)) repo)
        content-url (when gh-repo
                      (clojure.string/replace
                       (:contents_url gh-repo)
                       #"\{\+path\}" "bot.clj"))
        update (assoc bot :contents-url content-url)]
    (cond
      bot-registered? (bad-request! (str "Bot '" repo "' is already registered."))
      (not gh-repo)   (bad-request! (str "Could not find your repo '" repo "'."))
      :else           (ok (do
                            (db/add-player-bot _id update)
                            update)))))

(defn remove-player-bot
  [{:keys [_id]} repo]
  (db/remove-player-bot _id repo)
  (ok (str "Removed bot repo '" repo "'.")))
