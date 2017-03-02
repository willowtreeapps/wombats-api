(ns wombats.game.initializers
  (:require [base64-clj.core :as b64]
            [clj-time.core :as t]
            [cheshire.core :as cheshire]
            [clojure.core.async :as async]
            [org.httpkit.client :as http]
            [wombats.arena.utils :as a-utils]
            [wombats.game.utils :as g-utils]
            [wombats.game.zakano-code :refer [get-zakano-code]]
            [wombats.constants :refer [github-repo-api-base]]))

(defn- add-players-to-game
  "Adds players to random cells in the arena"
  [{:keys [players frame arena-config] :as game-state}]

  (update-in
   game-state
   [:frame :frame/arena]
   (fn [arena]
     (reduce
      (fn [new-arena [player-uuid {:keys [player]}]]
        (let [formatted-player (-> (:wombat a-utils/arena-items)
                                   (merge {:uuid player-uuid
                                           :color (:player/color player)
                                           :hp (:arena/wombat-hp arena-config)
                                           :orientation (g-utils/rand-orientation)}))]
          (a-utils/sprinkle new-arena formatted-player)))
      arena
      players))))

(defn- is-decision-maker?
  "Checks if the an item is capable of making decisions"
  [item]
  (let [decision-makers #{:wombat :zakano}]
    (contains? decision-makers (g-utils/get-item-type item))))

(defn- set-initiative-order
  "Sets the initial initiative order"
  [{:keys [frame] :as game-state}]
  (let [decision-makers (reduce
                         (fn [order item]
                           (if (is-decision-maker? item)
                             (conj order (select-keys (:contents item) [:uuid :type]))
                             order))
                         [] (flatten (:frame/arena frame)))]

    (assoc game-state
           :initiative-order
           (shuffle decision-makers))))

(defn- set-zakano-state
  [game-state]
  (reduce (fn [game-state-acc {:keys [uuid type]}]
            (if (= :zakano type)
              (assoc-in game-state-acc
                        [:zakano uuid]
                        {:state g-utils/decision-maker-state})
              game-state-acc))
          game-state (:initiative-order game-state)))

(defn- get-player-channels
  "Returns a seq of channels that are responsible for fetching user code"
  [players]
  (map (fn [[player-eid {:keys [wombat user]}]]
         (let [url (str github-repo-api-base (:wombat/url wombat))
               auth-headers {:headers {"Accept" "application/json"
                                       "Authorization" (str "token "
                                                            (:user/github-access-token user))}}
               ch (async/chan 1)]
           (async/go
             (let [resp @(http/get url auth-headers)]
               (async/>! ch {:player-eid player-eid
                             :resp resp})))
           ch))
       players))

(defn- decode-bot
  [encoded]
  (let [base64-string (clojure.string/replace encoded #"\n" "")]
    (b64/decode base64-string "UTF-8")))

(defn- parse-github-code
  [resp]
  (let [body (:body resp)
        parsed (cheshire/parse-string body true)]
    {:code (decode-bot (:content parsed))
     :path (:path parsed)}))

(defn- parse-player-channels
  "If the network request succeeded, attaches a users code to game-state"
  [players responses]
  (reduce (fn [player-acc {:keys [player-eid resp] :as player}]
            (if (= 200 (:status resp))
              (assoc-in player-acc [player-eid :state :code] (parse-github-code resp))
              player-acc))
          players
          responses))

(defn- source-player-code
  "Kicks off the code source process"
  [{:keys [players] :as game-state}]
  (let [bot-chans (get-player-channels players)
        responses (async/<!! (async/map vector bot-chans))]
    (update game-state :players parse-player-channels responses)))

(defn- source-zakano-code
  "Sources a bot to run as the zakano"
  [game-state]
  (let [;; TODO put zakano in db, maybe allow for selecting specific zakano?
        ;; url "https://api.github.com/repos/willowtreeapps/wombats-bots/contents/zakano.clj"
        ;; response @(http/get url)
        ;; code (parse-github-code response)
        code {:code (get-zakano-code)
              :path "zakano.clj"}]
    (update game-state :zakano (fn [zakano]
                                 (reduce (fn [zakano-acc [zakano-id zakano-state]]
                                           (assoc zakano-acc
                                                  zakano-id
                                                  (assoc-in zakano-state [:state :code] code)))
                                         {} zakano)))))

(defn initialize-game-state
  "Prepares the raw built up game state for the frame processor by adding required
  information to context."
  [game-state]
  (-> game-state
      (add-players-to-game)
      (set-initiative-order)
      (set-zakano-state)
      (source-player-code)
      (source-zakano-code)))

(defn- set-round-start-time
  "Sets the round start time"
  [game-state]
  (assoc-in game-state [:frame :frame/round-start-time] (->> (t/now)
                                                             (format "#inst \"%s\"")
                                                             (read-string))))

(defn- set-round-status
  "Sets the round status"
  [game-state]
  (assoc-in game-state [:game-config :game/status] :active))

(defn initialize-round
  "Adds round metadata to game state"
  [game-state]
  (-> game-state
      (set-round-start-time)
      (set-round-status)))

(defn- update-initiative-order
  "Rotates the initiative order"
  [initiative-order]
  (concat (take-last 1 initiative-order)
          (drop-last 1 initiative-order)))

(defn initialize-frame
  "Adds frame metadata to game state"
  [game-state]
  (-> game-state
      (update-in [:frame :frame/frame-number] inc)
      (update :initiative-order update-initiative-order)))
