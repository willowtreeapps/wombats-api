(ns wombats.test.arena.core
  (:require [clojure.test :refer :all]
            [wombats.arena.utils :as a-utils]
            [wombats.game.utils :as g-utils]
            [wombats.arena.core :as a-core]))

(def wide-arena-configuration
  #:arena {:food 0
           :poison 0
           :zakano 0
           :perimeter false
           :wood-walls 0
           :steel-walls 0
           :width 100
           :height 10})

(def tall-arena-configuration
  #:arena {:food 0
           :poison 0
           :zakano 0
           :perimeter false
           :wood-walls 0
           :steel-walls 0
           :width 10
           :height 100})

(def empty-arena-configuration
  #:arena{:food 0
          :poison 0
          :zakano 0
          :perimeter false
          :wood-walls 0
          :steel-walls 0
          :width 10
          :height 10})

(def perimeter-arena-configuration
  #:arena{:food 0
          :poison 0
          :zakano 0
          :perimeter true
          :wood-walls 0
          :steel-walls 0
          :width 10
          :height 10})

(deftest generate-arena
  (testing "passing a valid configuration map will create an arena of the proper dimensions"
    (is (= (-> (a-core/generate-arena empty-arena-configuration)
               (a-utils/get-arena-dimensions))
           [10 10]))
    (is (= (-> (a-core/generate-arena wide-arena-configuration)
               (a-utils/get-arena-dimensions))
           [100 10]))
    (is (= (-> (a-core/generate-arena tall-arena-configuration)
               (a-utils/get-arena-dimensions))
           [10 100])))

  (testing "passing a vaild configuration map will genereate an empty arena if no values are present"
    (is (= (->> (a-core/generate-arena empty-arena-configuration)
               (flatten)
               (filter #(= (get-in % [:contents :type]) :open))
               (count))
           100)))
  (testing "passing the perimeter configuration value will add a wood wall perimeter"
    (is (= (->> (a-core/generate-arena perimeter-arena-configuration)
                (flatten)
                (filter #(= (get-in % [:contents :type]) :wood-barrier))
                (count))
           36))
    (is (let [arena (a-core/generate-arena perimeter-arena-configuration)
              top-left (:type (g-utils/get-content-at-coords [0 0] arena))
              top-right (:type (g-utils/get-content-at-coords [9 0] arena))
              bottom-left (:type (g-utils/get-content-at-coords [0 9] arena))
              bottom-right (:type (g-utils/get-content-at-coords [9 9] arena))]
          (and (= :wood-barrier top-left)
               (= :wood-barrier top-right)
               (= :wood-barrier bottom-right)
               (= :wood-barrier bottom-right))))))
