(ns wombats.test.game.zakano-code
  (:require [clojure.test :refer :all]
            [wombats.arena.utils :refer [print-arena]]
            [wombats.game.zakano-code :as zc]))

(defonce sample-partial-arena-1
  (load-file "resources/zakano/sample-partial-arena-1.edn"))

(def ^:private sample-state
  {:local-coords [3 3]
   :global-coords [10 17]
   :global-dimensions [20 20]
   :arena sample-partial-arena-1
   :saved-state {}})

(defn- benchmark
  "Benchmarks fn for testing algorithms"
  {:added "1.0"}
  [my-function]
  (time (my-function))
  :done)

(deftest to-global-coords
  (let [to-global-coords-fn (zc/to-global-coords sample-state)
        to-global-coords-fn-wrap (zc/to-global-coords (assoc sample-state :global-coords [18 18]))]
    (testing "higher-order function that returns a function used to adjust coords"
      (is (function? to-global-coords-fn)))
    (testing "properly adjusts coords when passed local coords"
      (is (= [13 17] (to-global-coords-fn [6 3])))
      (is (= [13 14] (to-global-coords-fn [6 0])))
      (is (= [8 16] (to-global-coords-fn [1 2]))))
    (testing "properly adjusts coords when they wrap around the arena"
      (is (= [18 1] (to-global-coords-fn-wrap [3 6])))
      (is (= [16 1] (to-global-coords-fn-wrap [1 6])))
      (is (= [1 1] (to-global-coords-fn-wrap [6 6]))))))

(deftest has-next-action?
  (testing "returns true if there is an action inside of the remaining-action-seq prop"
    (is (= true (zc/has-next-action? {:remaining-action-seq [{:action :move}]}))))
  (testing "returns false if there remaining-action-seq is nil"
    (is (= false (zc/has-next-action? {:remaining-action-seq nil}))))
  (testing "returns false when remaining-action-seq is empty"
    (is (= false (zc/has-next-action? {:remaining-action-seq []})))))

(deftest get-first-frontier
  (testing "returns the correct \"first\" frontier"
    (is (= {:coords [3 3]
            :orientation :e
            :uuid "2d0e8172-d47b-4b48-8825-8e3326e1a3d7"
            :weight 0
            :action-sequence []}
           (zc/get-first-frontier sample-state)))))

(deftest caclulate-turn-frontiers
  (let [frontier (zc/get-first-frontier sample-state)]
    (testing "Returns the three alternate orientations for a frontier"
      (is (= #{:n :s :w} (set (map #(:orientation %)
                                   (zc/calculate-turn-frontiers frontier))))))))

(deftest get-move-frontier-coords
  (testing "wraps around the arena when passed wrap? true"
    (is (= [0 6] (zc/get-move-frontier-coords [6 6] :e [7 7] true)))
    (is (= [6 0] (zc/get-move-frontier-coords [6 6] :s [7 7] true)))
    (is (= [6 0] (zc/get-move-frontier-coords [0 0] :w [7 7] true)))
    (is (= [0 6] (zc/get-move-frontier-coords [0 0] :n [7 7] true))))
  (testing "returns nil when moving past the edge of the arena and wrap? is false"
    (is (= nil (zc/get-move-frontier-coords [6 6] :e [7 7] false)))
    (is (= nil (zc/get-move-frontier-coords [6 6] :s [7 7] false)))
    (is (= nil (zc/get-move-frontier-coords [0 0] :n [7 7] false)))
    (is (= nil (zc/get-move-frontier-coords [0 0] :w [7 7] false)))))

(deftest caclulate-move-frontier
  (testing "returns a new frontier with updated coords, weight, and action-sequence"
    (is (= {:orientation :e
            :coords [4 3]
            :weight 1
            :action-sequence [{:action :move}]}
           (zc/calculate-move-frontier (zc/get-first-frontier sample-state)
                                       [7 7]
                                       false))))

  (testing "returns nil when move is out of bounds"
    (is (= nil
           (zc/calculate-move-frontier (assoc (zc/get-first-frontier sample-state)
                                              :coords
                                              [6 3])
                                       [7 7]
                                       false)))))

(deftest filter-frontiers
  (let [frontiers [nil {:coords [3 3]} {:coords [0 0]}]]
    (testing "returns a frontier list that is explore-able"
      (is (= [{:coords [3 3]} {:coords [0 0]}]
             (zc/filter-frontiers frontiers
                                  (:arena sample-state)
                                  {})))
      (is (= [{:coords [0 0]}]
             (zc/filter-frontiers frontiers
                                  (:arena sample-state)
                                  {"2d0e8172-d47b-4b48-8825-8e3326e1a3d7" true}))))))

(deftest modify-orientation
  (testing "returns the proper orientation shift given a command"
    (is (= :n (zc/modify-orientation :e :left)))
    (is (= :w (zc/modify-orientation :n :left)))
    (is (= :s (zc/modify-orientation :w :left)))
    (is (= :e (zc/modify-orientation :s :left)))
    (is (= :s (zc/modify-orientation :e :right)))
    (is (= :e (zc/modify-orientation :n :right)))
    (is (= :n (zc/modify-orientation :w :right)))
    (is (= :w (zc/modify-orientation :s :right)))
    (is (= :w (zc/modify-orientation :e :about-face)))
    (is (= :s (zc/modify-orientation :n :about-face)))
    (is (= :e (zc/modify-orientation :w :about-face)))
    (is (= :n (zc/modify-orientation :s :about-face)))))


(benchmark #(zc/main-fn (assoc sample-state
                               :saved-state
                               (:state
                                (zc/main-fn sample-state (fn [])))) (fn [])))
(clojure.pprint/pprint (zc/main-fn sample-state (fn [])))
(benchmark #(zc/main-fn sample-state (fn [])))
