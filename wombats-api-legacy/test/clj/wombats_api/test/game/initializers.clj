(ns wombats-api.test.game.initializers
  (:require [wombats-api.game.initializers :refer :all :as initializers]
            [wombats-api.test.game.test-game :refer [o f p b b1 b2 a]])
  (:use clojure.test))

(deftest update-cell-metadata-spec
  (is (= (assoc o :md {:1 {:type :shot
                           :decay 1}})
         (#'initializers/update-cell-metadata (assoc o :md {:1 {:type :shot
                                                                :decay 2}})))
      "A cell's metadata decay value is decremented")
  (is (= (assoc o :md {})
         (#'initializers/update-cell-metadata (assoc o :md {:1 {:type :shot
                                                                :decay 1}})))
      "When a cell's metadata decay value drops to or below 0, that particular piece of metadata will be removed")
  (is (= (assoc o :md {:2 {:type :shot
                           :decay 1}})
         (#'initializers/update-cell-metadata (assoc o :md {:1 {:type :shot
                                                                :decay 1}
                                                            :2 {:type :shot
                                                                :decay 2}})))
      "Only metadata that has reached it's decay point will be removed"))

(deftest udpate-volatile-cells-spec
  (is (= [[(assoc o :md {})
           (assoc o :md {})]
          [(assoc o :md {:2 {:type :shot
                             :decay 1}})
           (assoc o :md {})]]
         (#'initializers/update-volatile-cells [[(assoc o :md {:1 {:type :shot
                                                                   :decay 1}})
                                                 (assoc o :md {:1 {:type :shot
                                                                   :decay 1}})]
                                                [(assoc o :md {:2 {:type :shot
                                                                   :decay 2}})
                                                 (assoc o :md {:3 {:type :shot
                                                                   :decay 0}})]]))))
