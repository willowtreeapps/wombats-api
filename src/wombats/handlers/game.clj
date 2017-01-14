(ns wombats.handlers.game
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]))

(def game {:name :some-name
           :type :free-for-all})

(defbefore get-games
  [{:keys [response] :as context}]
  (let [ch (chan 1)]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 200
                                             :body game))))
    ch))

(defbefore add-game
  [{:keys [response] :as context}]
  (let [ch (chan 1)]
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 201
                                             :body game))))
    ch))
