(ns battlebots.game.finalizers_spec
  (:require [battlebots.game.finalizers :refer :all :as finalizers]
            [battlebots.game.test-game :refer [o f p b b1 b2 a]])
  (:use clojure.test))

(deftest update-cell-metadata-spec
  (is (= (assoc o :md {:1 {:type :shot
                           :decay 0}})
         (#'finalizers/update-cell-metadata (assoc o :md {:1 {:type :shot
                                                              :decay 1}})))
      "A cell's metadata decay value is decremented")
  (is (= (assoc o :md {})
         (#'finalizers/update-cell-metadata (assoc o :md {:1 {:type :shot
                                                              :decay 0}})))
      "When a cell's metadata decay value drops below 0, that particular piece of metadata will be removed")
  (is (= (assoc o :md {:2 {:type :shot
                           :decay 0}})
         (#'finalizers/update-cell-metadata (assoc o :md {:1 {:type :shot
                                                              :decay 0}
                                                          :2 {:type :shot
                                                              :decay 1}})))
      "Only metadata that has reached it's decay point will be removed"))

(deftest udpate-volatile-cells-spec
  (is (= [[(assoc o :md {:1 {:type :shot
                             :decay 0}})
           (assoc o :md {:1 {:type :shot
                             :decay 0}})]
          [(assoc o :md {:2 {:type :shot
                             :decay 1}})
           (assoc o :md {})]]
         (update-volatile-cells [[(assoc o :md {:1 {:type :shot
                                                    :decay 1}})
                                  (assoc o :md {:1 {:type :shot
                                                    :decay 1}})]
                                 [(assoc o :md {:2 {:type :shot
                                                    :decay 2}})
                                  (assoc o :md {:3 {:type :shot
                                                    :decay 0}})]]))))
