(ns battlebots.game.messages-spec
  (:require [battlebots.game.messages :refer :all :as messages]
            [battlebots.game.test-game :refer [test-players b1 b2 b
                                               bot1-private
                                               bot2-private
                                               test-game-state]])
  (:use clojure.test))

(deftest add-player-message-spec
  (is (= [{:type :info
           :message "Some Message"}] (:messages (first (#'messages/add-player-message "1"
                                                                                      test-players
                                                                                      {:type :info
                                                                                       :message "Some Message"}))))
      "Adds a message to a players message collection"))

(deftest log-collision-event-spec
  (is (= 1 (count (:messages ((log-collision-event "1" b2 10) test-game-state))))
      "A frame message is added when there is a collision")
  (is (= 1 (count (:messages (first (:players ((log-collision-event "1" b 10) test-game-state))))))
      "When a bot collides with an object, a player message is logged")
  (is (some? (and (= 1 (count (:messages (first (:players ((log-collision-event "1" b1 10) test-game-state))))))
                  (= 1 (count (:messages (last (:players ((log-collision-event "1" b 10) test-game-state))))))))
      "When a bot collides with another bot, a player message is logged with both bots"))
