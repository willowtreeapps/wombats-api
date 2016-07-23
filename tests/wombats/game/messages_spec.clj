(ns wombats.game.messages-spec
  (:require [wombats.game.messages :refer :all :as messages]
            [wombats.game.test-game :refer [test-players b1 b2 b
                                               bot1-private
                                               bot2-private
                                               test-game-state]])
  (:use clojure.test))

(deftest add-messages-spec
  (is (= {:messages {:global ["This message will be displayed to all users"]
                     :1234 ["This message will be displayed to user 1234"]}}
         (#'messages/add-messages {:messages {}}
                                  [{:chan :global
                                    :message "This message will be displayed to all users"}
                                   {:chan "1234"
                                    :message "This message will be displayed to user 1234"}]))
      "Messages are add to their respecive channels")
  (is (= {:messages {:global ["This is an old global message"
                              "This message will be displayed to all users"]
                     :1234 ["This message will be displayed to user 1234"]}}
         (#'messages/add-messages {:messages {:global ["This is an old global message"]}}
                                  [{:chan :global
                                    :message "This message will be displayed to all users"}
                                   {:chan "1234"
                                    :message "This message will be displayed to user 1234"}]))
      "New messages are added to existing message collections"))
