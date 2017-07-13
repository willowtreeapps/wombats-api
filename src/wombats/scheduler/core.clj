(ns wombats.scheduler.core
  (:require [chime :refer [chime-at]]
            [taoensso.timbre :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.periodic :as p]
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

(defn add-game-scheduler
  "Called by the scheduler to add a game that starts a day after the scheduled time"
  [{:keys [initial-time
           game-params
           add-game-fn
           gen-id-fn
           get-game-by-id-fn
           get-arena-by-id-fn
           start-game-fn]}]
  (let [arena-id (:arena-id game-params)
        time-game (str (t/plus initial-time (t/hours 24)))
        game (merge (:game game-params)
                    {:game/start-time
                     (read-string (str "#inst \"" time-game "\""))})
        game-id (gen-id-fn)
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
     start-game-fn)))

(defn automatic-game-scheduler
  "Called on system start - uses add-game-scheduler to create a game"
  [setup-params]
  (chime-at (rest ; excludes *right now*
               (p/periodic-seq (t/now)
                               (-> 24 t/hours)))

          (fn [time]
            (add-game-scheduler
             setup-params)
            (log/info "Scheduled a new daily game"))

          {:on-finished #(log/info "Stopped scheduling daily games")}))
