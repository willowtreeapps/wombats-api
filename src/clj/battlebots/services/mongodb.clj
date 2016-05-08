(ns battlebots.services.mongodb
  (:require [monger.core :as mg]))

(def conn (atom (mg/connect-via-uri "mongodb://127.0.0.1/monger-test")))

(defn get-db [] (:db @conn))
