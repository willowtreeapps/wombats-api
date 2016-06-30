(ns battlebots.game_spec
  (:require [battlebots.game :as game]
            [battlebots.arena.utils :refer [get-item]]
            [battlebots.constants.arena :refer [arena-key]])
  (:use clojure.test))

(def test-players [{:_id "1"
                    :login "oconn"
                    :bot-repo "bot"
                    :energy 20
                    :bot "{:commands [{:cmd \"MOVE\"
                                       :metadata {:direction (rand-nth [0])}}
                                      {:cmd \"SET_STATE\"
                                       :metadata {:step-counter 0}}]}"
                    :saved-state {}}
                   {:_id "2"
                    :login "Mr. Robot"
                    :bot-repo "bot"
                    :energy 50
                    :bot "{:commands [{:cmd \"MOVE\"
                                       :metadata {:direction (rand-nth [0])}}
                                      {:cmd \"SET_STATE\"
                                       :metadata {:step-counter 0}}]}"
                    :saved-state {}}])

(def o (:open arena-key))
(def b (:block arena-key))
(def f (:food arena-key))
(def p (:poison arena-key))

(def test-arena [[o o b f p f f]
                 [b f f p o o o]
                 [b f o o p f p]
                 [b f f p o b {:_id "1" :login "oconn"}]
                 [b p o o o p {:_id "2" :login "Mr. Robot"}]
                 [p o p f f f f]
                 [o o o o f p f]])

(deftest position-spec
  (is (= 1 (#'game/position #(= % 4) [0 4 5 6])))
  (is (= 3 (#'game/position #(= % 6) [0 4 5 6])))
  (is (= nil (#'game/position #(= % 7) [0 4 5 6]))))

(deftest is-player-spec
  (is (= true (#'game/is-player? {:login "somelogin"})))
  (is (= false (#'game/is-player? {:no "login here"}))))

(deftest update-player-with-spec
  (is (= 1000 (:score (first (#'game/update-player-with "1"
                                                        test-players
                                                        {:score 1000}))))))

(deftest total-rounds-spec
  (is (= 75 (#'game/total-rounds 15 2)))
  (is (= 103 (#'game/total-rounds 13 3))))

(deftest get-player-spec
  (is (= (first test-players) (#'game/get-player "1" test-players))))

(deftest sanitize-player-spec
  (is (= {:_id "1"
          :login "oconn"} (#'game/sanitize-player (first test-players)))))

(comment (deftest save-segment-spec))

(deftest randomize-players-spec
  (is (= (count test-players)
         (count (set (repeatedly 100 (partial #'game/randomize-players test-players))))))
  "Players are randomized. NOTE: While not likely, this test could fail if one permutation
  is not calculated.")

(deftest get-player-coords-spec
  (is (= [6 3] (#'game/get-player-coords "1" test-arena)))
  (is (= [6 4] (#'game/get-player-coords "2" test-arena))))

(deftest can-occupy-space-spec
  (is (= true (#'game/can-occupy-space? {:type "food"})) "Bots can occupy food spaces")
  (is (= true (#'game/can-occupy-space? {:type "poison"})) "Bots can occupy poison spaces")
  (is (= true (#'game/can-occupy-space? {:type "open"})) "Bots can occupy open spaces")
  (is (= false (#'game/can-occupy-space? {:type "block"}))) "Bots cannot occupy block spaces")

(comment (deftest determin-effects-spec
           (is (= {:energy #(+ % 10)} (#'game/determine-effects f)))))

(deftest apply-player-update-spec
  (is (= {:energy 5} (#'game/apply-player-update {:energy 10} {:energy #(- % 5)})))
  (is (= (assoc (first test-players) :energy 5)
         (#'game/apply-player-update (first test-players) {:energy #(- % 15)}))))

(deftest modify-player-state-spec
  (is (= (assoc-in test-players [0 :energy] 1)
         (#'game/modify-player-stats "1" {:energy #(- % 19)} test-players))))

(deftest apply-damage-spec
  (is (= o (#'game/apply-damage b 100))
      "Items are replaced with open spaces when they are destroyed.")
  (is (= (assoc b :energy (- (:energy b) 100)) (#'game/apply-damage b 100 false))
      "When false is passed as the last argument, the item is not replaced with an open space")
  (is (= (assoc b :energy (dec (:energy b))) (#'game/apply-damage b 1))
      "An item's energy is updated when damage is applied"))
