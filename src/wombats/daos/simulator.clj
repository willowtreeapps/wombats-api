(ns wombats.daos.simulator
  (:require [wombats.daos.helpers :refer [get-entities-by-prop]]
            [taoensso.nippy :as nippy]))

(defn get-simulator-arena-templates
  [conn]
  (fn []
    (map #(update % :simulator-template/arena nippy/thaw)
         (get-entities-by-prop conn :simulator-template/id))))
