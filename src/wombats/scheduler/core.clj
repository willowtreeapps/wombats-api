(ns wombats.scheduler.core
  (:require [chime :refer [chime-at]]
            [taoensso.timbre :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [wombats.arena.core :refer [generate-arena]]))

(defn schedule-game
  "Takes in a game, and adds a hook to start the game at start"
  [game-id start-time start-round-fn]
  (try
    (if (t/after? (t/now) (c/from-date start-time))
      ;; Start time has already passed
      (start-round-fn game-id)
      ;; Set interval to start game in the future
      (chime-at [(-> start-time)]
                (fn [time]
                  (start-round-fn game-id))
                {:on-finished
                 (fn []
                   (log/info (str "Starting scheduled round in game " game-id)))
                 :error-handler
                 (fn [e]
                   (log/error (str "Error processing scheduled game " game-id)))}))
    (catch Exception e
      (log/error (str "Error scheduling game " game-id)))))

(defn schedule-next-round
  [game-state round-start-fn]
  (let [{game-status :game/status
         game-id :game/id} game-state
        start-time (get-in game-state [:game/frame :frame/round-start-time])]
    (when (= game-status :active-intermission)
      (schedule-game game-id start-time round-start-fn)))
  game-state)

(defn schedule-pending-games
  [get-games-fn start-round-fn]
  (let [games (get-games-fn)]
    (doseq [game games]
      (schedule-game (:game/id game)
                     (:game/start-time game)
                     start-round-fn))))


;; TODO #349
;; Schedule active & active intermission games
;; in the case of system restart.

(def add-game-request
  {:arena-id "41193bec-4cf3-4f5d-8386-aed3ae3e5745"
   :game
   {:game/start-time "2017-06-12T21:55:00.000-00:00"
    :game/status :pending-open
    :game/num-rounds 8
    :game/round-length 120000
    :game/round-intermission 600000
    :game/max-players 8
    :game/password ""
    :game/is-private false
    :game/type :high-score
    :game/name "Daily Game"}})

(defn add-game-scheduler
  [request add-game-fn gen-id-fn get-game-by-id-fn get-arena-by-id-fn start-game-fn]
  (let [arena-id (:arena-id request)
        time-game (str (t/today-at 23 00))
        game (merge (:game request)
                    {:game/start-time
                     (read-string (str "#inst \"" time-game "\""))})
        ]
    (println (:game/start-time game))

    (let [game-id gen-id-fn
          _broke (println game-id)
          new-game (merge game {:game/id game-id})
          arena-config (get-arena-by-id-fn arena-id)
          game-arena (generate-arena arena-config)
          tx @(add-game-fn new-game
                        (:db/id arena-config)
                        game-arena)
          game-record (get-game-by-id-fn game-id)]
      (schedule-game
       game-id
       (:game/start-time game-record)
       start-game-fn)
      ;; todo return game-record here
      )))

;; Post request from frontend for the context when creating game
#_(POST (create-game-url arena-id)
        {:response-format (edn-response-format)
         :keywords? true
         :format (edn-request-format)
         :headers (add-auth-header {})
         :params {:game/start-time start-time
                  :game/num-rounds num-rounds
                  :game/round-length round-length
                  :game/round-intermission round-intermission
                  :game/max-players max-players
                  :game/password password
                  :game/is-private is-private
                  :game/type game-type
                  :game/name name}
         :handler on-success
         :error-handler on-error})
