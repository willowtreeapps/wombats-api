(ns wombats.game.initializers
  (:require [clojure.core.async :as async]
            [org.httpkit.client :as http]
            [wombats.arena.utils :as a-utils]
            [wombats.game.utils :as g-utils]))

(defn- add-players-to-game
  "Adds players to random cells in the arena"
  [{:keys [players frame] :as game-state}]

  (update-in
   game-state
   [:frame :frame/arena]
   (fn [arena]
     (reduce
      (fn [new-arena [_ {:keys [player stats]}]]
        (let [formatted-player (-> (:wombat a-utils/arena-items)
                                   (a-utils/ensure-uuid)
                                   (merge {:player-eid (:db/id player)
                                           :color (:player/color player)
                                           :hp (:stats/hp stats)
                                           :orientation (g-utils/rand-orientation)}))]
          (a-utils/sprinkle new-arena formatted-player)))
      arena
      players))))

(defn- update-initiative-order
  "Rotates the initiative order"
  ([initiative-order]
   (concat (take-last 1 initiative-order)
           (drop-last 1 initiative-order)))
  ([initiative-order players]
   (->> players
        (map (fn [[_ {:keys [player]}]]
               (:db/id player)))
        (shuffle))))

(defn- set-initiative-order
  "Sets the initial initiative order"
  [{:keys [players] :as game-state}]
  (assoc game-state
         :initiative-order
         (update-initiative-order nil players)))

(defn- get-bot-channels
  "Returns a seq of channels that are responsible for fetching user code"
  [players]
  (map (fn [[player-eid {:keys [wombat user]}]]
         (let [url (str "https://api.github.com/repos" (:wombat/url wombat))
               auth-headers {:headers {"Accept" "application/json"
                                       "Authorization" (str "token " (:user/access-token user))}}
               ch (async/chan 1)]
           (async/go
             (let [resp @(http/get url auth-headers)]
               (async/>! ch {:player-eid player-eid
                             :resp resp})))
           ch))
       players))

(defn- parse-bot-channels
  "If the network request succeeded, attaches a users code to game-state"
  [players responses]
  (reduce (fn [player-acc {:keys [player-eid resp]}]
            (when (= 200 (:status resp))
              (assoc-in player-acc [player-eid :code] (:body resp))))
          players
          responses))

(defn- source-user-code
  "Kicks off the code source process"
  [{:keys [players] :as game-state}]
  (let [bot-chans (get-bot-channels players)
        responses (async/<!! (async/map vector bot-chans))]
    (update game-state :players parse-bot-channels responses)))

(defn initialize-game
  [game-state]
  (-> game-state
      (add-players-to-game)
      (set-initiative-order)
      (source-user-code)))

(defn initialize-frame
  [game-state]
  (-> game-state
      (update-in [:frame :frame/frame-number] inc)
      (update :initiative-order update-initiative-order)))
