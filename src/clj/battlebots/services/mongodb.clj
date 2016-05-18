(ns battlebots.services.mongodb
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:import org.bson.types.ObjectId))

(def conn (atom (mg/connect-via-uri "mongodb://127.0.0.1/battlebots")))

(defn get-db [] (:db @conn))

(defn find-one
  "searches for a single record, if it finds one it returns it as a map, otherwise nil"
  [collection-name _id]
  (mc/find-one-as-map (get-db) collection-name {:_id (ObjectId. _id)}))

(defn find-all
  "returns all records of a given collection"
  [collection-name]
  (mc/find-maps (get-db) collection-name))

(defn insert-one
  "inserts a single record into a given collection"
  [collection-name record]
  (let [_id (ObjectId.)
        insert-record (assoc record :_id _id)]
    (mc/insert (get-db) collection-name insert-record)
    insert-record))

(defn remove-one
  "removes a single record by given id"
  [collection-name _id]
  (mc/remove-by-id (get-db) collection-name (ObjectId. _id)))
