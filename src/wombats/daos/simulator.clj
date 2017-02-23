(ns wombats.daos.simulator
  (:require [wombats.daos.helpers :refer [get-entities-by-prop
                                          get-entity-by-prop]]
            [taoensso.nippy :as nippy]))

(defn get-simulator-arena-templates
  [conn]
  (fn []
    (get-entities-by-prop conn :simulator-template/id '[:simulator-template/id
                                                        :simulator-template/name
                                                        {:simulator-template/arena-template [*]}])))

(defn get-simulator-arena-template-by-id
  [conn]
  (fn [template-id]
    (get-entity-by-prop conn
                        :simulator-template/id
                        template-id
                        '[*
                          {:simulator-template/arena-template [*]}])))
