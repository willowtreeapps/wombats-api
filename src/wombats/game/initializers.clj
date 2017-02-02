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
      (fn [new-arena [player-uuid {:keys [player stats]}]]
        (let [formatted-player (-> (:wombat a-utils/arena-items)
                                   (merge {:uuid player-uuid
                                           :color (:player/color player)
                                           ;; TODO Pull from config
                                           :hp 100
                                           :orientation (g-utils/rand-orientation)}))]
          (a-utils/sprinkle new-arena formatted-player)))
      arena
      players))))

(defn- update-initiative-order
  "Rotates the initiative order"
  [initiative-order]
  (concat (take-last 1 initiative-order)
          (drop-last 1 initiative-order)))

(defn- is-decision-maker?
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

(defn- parse-player-channels
  "If the network request succeeded, attaches a users code to game-state"
  [players responses]
  (reduce (fn [player-acc {:keys [player-eid resp]}]
            (when (= 200 (:status resp))
              (assoc-in player-acc [player-eid :state :code] (:body resp))))
          players
          responses))

(defn- source-player-code
  "Kicks off the code source process"
  [{:keys [players] :as game-state}]
  (let [bot-chans (get-player-channels players)
        responses (async/<!! (async/map vector bot-chans))]
    (update game-state :players parse-player-channels responses)))

(defn initialize-game
  [game-state]
  (-> game-state
      (add-players-to-game)
      (set-initiative-order)
      (set-zakano-state)
      (source-player-code)))

(defn initialize-frame
  [game-state]
  (-> game-state
      (update-in [:frame :frame/frame-number] inc)
      (update :initiative-order update-initiative-order)))
