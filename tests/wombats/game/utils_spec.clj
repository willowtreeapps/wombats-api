(ns wombats.game.utils_spec
  (:require [wombats.game.utils :refer :all :as gu]
            [wombats.game.test-game :refer [test-players test-arena o b]])
  (:use clojure.test))

(deftest position-spec
  (is (= 1 (position #(= % 4) [0 4 5 6])))
  (is (= 3 (position #(= % 6) [0 4 5 6])))
  (is (= nil (position #(= % 7) [0 4 5 6]))))

(deftest is-player-spec
  (is (= true (is-player? {:type "player"})))
  (is (= false (is-player? {:type "food"}))))

(deftest get-player-spec
  (is (= (first test-players) (get-player "1" test-players))))

(deftest update-player-with-spec
  (is (= 1000 (:score (first (update-player-with
                              "1"
                              test-players
                              {:score 1000}))))))

(deftest get-item-coords-spec
  (is (= [6 3] (get-item-coords "1111-1111-1111-1111" test-arena)))
  (is (= [6 4] (get-item-coords "2222-2222-2222-2222" test-arena))))

(deftest get-player-coords-spec
  (is (= [6 3] (get-player-coords "1" test-arena)))
  (is (= [6 4] (get-player-coords "2" test-arena))))

(deftest sanitize-player-spec
  (is (= {:_id "1"
          :uuid "1111-1111-1111-1111"
          :login "oconn"
          :hp 100} (sanitize-player {:_id "1"
                                         :uuid "1111-1111-1111-1111"
                                         :login "oconn"
                                         :hp 100
                                         :super-secret-value "shhhh...."}))))

(deftest modify-player-state-spec
  (is (= (assoc-in test-players [0 :hp] 1)
         (modify-player-stats "1" {:hp #(- % 19)} test-players))))

(deftest apply-damage-spec
  (is (= o (apply-damage b 100))
      "Items are replaced with open spaces when they are destroyed.")
  (is (= (assoc b :hp (- (:hp b) 100)) (apply-damage b 100 false))
      "When false is passed as the last argument, the item is not replaced with an open space")
  (is (= (assoc b :hp (dec (:hp b))) (apply-damage b 1))
      "An item's hp is updated when damage is applied"))
