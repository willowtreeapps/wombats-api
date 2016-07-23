(ns wombats.prod
  (:require [wombats.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init)
