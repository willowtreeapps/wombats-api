(ns wombats.daos.access-key
  (:require [datomic.api :as d]
            [wombats.daos.helpers :refer [get-entities-by-prop
                                          get-entity-by-prop
                                          retract-entity-by-prop]]))

(defn get-access-keys
  [conn]
  (fn []
    (get-entities-by-prop conn :access-key/id)))

(defn get-access-key-by-id
  [conn]
  (fn [id]
    (get-entity-by-prop conn :access-key/id id)))

(defn get-access-key-by-key
  [conn]
  (fn [key]
    (get-entity-by-prop conn :access-key/key key)))

(defn add-access-key
  [conn]
  (fn [access-key]
    @(d/transact conn [[:add-access-key access-key]])))

(defn retract-access-key
  [conn]
  (fn [access-key-id]
    @(retract-entity-by-prop conn :access-key/id access-key-id)))

(defn update-access-key
  [conn]
  (fn [access-key-id]
    "NOT YET IMPLEMENTED. https://github.com/willowtreeapps/wombats-api/issues/341"))
