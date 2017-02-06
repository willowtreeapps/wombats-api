(ns wombats.game.core
  (:require [wombats.game.initializers :as i]
            [wombats.game.finalizers :as f]
            [wombats.game.processor :as p]
            [wombats.arena.utils :as au]
            [wombats.sockets.game :as game-sockets]))

(defn- game-over?
  "End game condition"
  [game-state]
  ;; TODO For now we're only calculating a fix number of rounds
  ;; This will have to be updated with the base condition for
  ;; each game type
  (= 10 (get-in game-state [:frame :frame/frame-number])))

(defn- push-frame-to-clients
  [game-state]

  (game-sockets/broadcast-arena
   (:game-id game-state)
   (get-in game-state [:frame :frame/arena]))

  game-state)

(defn- add-player-scores
  [stats players]
  (reduce
   (fn [stats-acc [uuid {:keys [stats user wombat]
                        :as player}]]
     (let [score (:stats/score stats)
           user (:user/github-username user)
           wombat (:wombat/name wombat)]
       (assoc stats-acc uuid {:score score
                              :username user
                              :wombat-name wombat})))
   stats players))

(defn- add-player-hp
  [stats arena]
  (let [player-ids (set (keys stats))]
    (reduce
     (fn [stats-acc cell]
       (let [cell-uuid (get-in cell [:contents :uuid])]
         (if (contains? player-ids cell-uuid)
           (assoc-in stats-acc [cell-uuid :hp] (get-in cell [:contents :hp]))
           stats-acc)))
     stats (flatten arena))))

(defn- push-stats-update-to-clients
  [game-state]

  (let [player-stats (-> {}
                         (add-player-scores (:players game-state))
                         (add-player-hp (get-in game-state [:frame
                                                            :frame/arena]))
                         vals
                         first)]
    (game-sockets/broadcast-stats
     (:game-id game-state)
     player-stats))
  game-state)

(defn frame-debugger
  "This is a debugger that will print out a ton of additional
  information in between each frame"
  [{:keys [frame] :as game-state} interval]

  ;; Pretty print the arena
  #_(au/print-arena (:frame/arena frame))

  ;; Pretty print the full arena state
  #_(clojure.pprint/pprint (:frame/arena frame))

  ;; Pretty print everything but the arena
  #_(clojure.pprint/pprint
   (dissoc game-state :frame))

  ;; Pretty print everything
  (clojure.pprint/pprint game-state)

  ;; Print number of players
  #_(prn (str "Player Count: " (count (keys (:players game-state)))))

  ;; Sleep before next frame
  (Thread/sleep interval)

  ;; Return game-state
  game-state)

(defn- timeout-frame
  "Pause the frame to allow the client to catch up"
  [game-state time]
  (Thread/sleep time)
  game-state)

(defn- game-loop
  "Game loop"
  [game-state aws-credentials]
  (loop [current-game-state game-state]
    (if (game-over? current-game-state)
      current-game-state
      (-> current-game-state
          (i/initialize-frame)
          (p/source-decisions aws-credentials)
          (p/process-decisions)
          (f/finalize-frame)
          #_(timeout-frame 500)
          (push-frame-to-clients)
          (push-stats-update-to-clients)
          #_(frame-debugger 1000)
          (recur)))))


(defn initialize-game
  "Main entry point for the game engine"
  [game-state aws-credentials]
  (-> game-state
      (i/initialize-game)
      #_(frame-debugger 0)
      (game-loop aws-credentials)
      (f/finalize-game)))
