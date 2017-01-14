(ns wombats-api.test.game.bot.decisions.shoot
  (:require [wombats-api.game.bot.decisions.shoot :refer :all :as shoot]
            [wombats-api.config.game :as gc]
            [wombats-api.constants.arena :as ac]
            [wombats-api.arena.utils :as au]
            [wombats-api.test.game.test-game :refer [o b
                                                     bot1-private
                                                     bot2-private
                                                     b1 b2
                                                     test-players
                                                     test-arena]])
  (:use clojure.test))

(deftest add-shot-metadata-spec
  (is (= {:hp 10
          :md {:1234 {:type :shot
                      :decay 1}}}
         ((#'shoot/add-shot-metadata "1234") {:hp 10}))
      "Adds shot metadata to a cell")
  (is (= {:hp 12
          :md {:1234 {:type :shot
                      :decay 1}
               :5678 {:type :explosion
                      :decay 5}}}
         ((#'shoot/add-shot-metadata "1234") {:hp 12
                                              :md {:5678 {:type :explosion
                                                          :decay 5}}}))
      "Adds shot metadata to a cell already containing metadata"))

(deftest add-shot-damage-spec
  (is (= {:hp 190} ((#'shoot/add-shot-damage 10) {:hp 200})))
  (is (= {:hp 12} ((#'shoot/add-shot-damage 15) {:hp 27}))))

(deftest replace-destroyed-cell-spec
  (is (= (assoc o :md {:1234 {:type :destroyed
                              :decay 1}})
         ((#'shoot/replace-destroyed-cell "1234") (assoc b :hp 0)))
      "A cell is replaced if it is destroyed")
  (is (= b
         ((#'shoot/replace-destroyed-cell "1234") b))
      "A cell is not modified if it is not destroyed"))

(deftest resolve-shot-cell-spec
  (is (= {:hp 5
          :md {:1234 {:type :shot
                      :decay 1}}}
         (#'shoot/resolve-shot-cell {:hp 20} 15 "1234")))
  (is (= {:hp -10
          :md {:1234 {:type :shot
                      :decay 1}}}
         (#'shoot/resolve-shot-cell {:hp 20} 30 "1234"))))

(deftest shot-should-progress-spec
  (is (= false (#'shoot/shot-should-progress? false (:open ac/arena-key) 10))
      "Returns false when should-progress? prop is false")
  (is (= false (#'shoot/shot-should-progress? true (:open ac/arena-key) 0))
      "Returns false when hp is 0")
  (is (= false (#'shoot/shot-should-progress? true (:open ac/arena-key) -10))
      "Returns false when hp is less than 0")
  (is (= false (#'shoot/shot-should-progress? true (:steel ac/arena-key) 10))
      "Returns false when encountering an cell it cannot pass through")
  (is (= true (#'shoot/shot-should-progress? true (:open ac/arena-key) 10))
      "Returns true when all of the above test cases return true"))

(deftest process-shot-spec
  (is (= {:game-state {:dirty-arena (au/update-cell
                                     test-arena
                                     [0 1]
                                     (merge b {:hp 10
                                               :md {:1234 {:type :shot
                                                           :decay 1}}}))
                       :players test-players}
          :shot-damage 0
          :should-progress? true
          :shot-uuid "1234"
          :shooter-id "99999"}
         (update-in (#'shoot/process-shot
                     {:game-state {:dirty-arena test-arena
                                   :players test-players}
                      :shot-damage 10
                      :should-progress? true
                      :shot-uuid "1234"
                      :shooter-id "99999"}
                     [0 1])
                    [:game-state] dissoc :messages))
      "If a shot passes through a cell what container more hp than is left in the shot. There should be no hp left over.")
  (is (= {:game-state {:dirty-arena (au/update-cell
                                     test-arena
                                     [0 1]
                                     (merge o {:md {:1234 {:type :destroyed
                                                           :decay 1}}}))
                       :players test-players}
          :shot-damage 12
          :should-progress? true
          :shot-uuid "1234"
          :shooter-id "99999"}
         (update-in (#'shoot/process-shot
                     {:game-state {:dirty-arena test-arena
                                   :players test-players}
                      :shot-damage 32
                      :should-progress? true
                      :shot-uuid "1234"
                      :shooter-id "99999"}
                     [0 1])
                    [:game-state] dissoc :messages))
      "If a shot contains more hp than a cell has, the delta hp should be returned in the shot state.")
  (is (= {:game-state {:dirty-arena test-arena
                       :players test-players}
          :shot-damage 32
          :should-progress? false
          :shot-uuid "1234"
          :shooter-id "99999"}
         (update-in (#'shoot/process-shot
                     {:game-state {:dirty-arena test-arena
                                   :players test-players}
                      :shot-damage 32
                      :should-progress? false
                      :shot-uuid "1234"
                      :shooter-id "99999"}
                     [0 1])
                    [:game-state] dissoc :messages))
      "If a shot should not progress, shot state is not updated"))
