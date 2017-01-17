(ns wombats.interceptors.swagger
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]))

(def annotation
  "Gets documentation from an annotated object"
  (comp ::doc meta))

(defbefore get-docs-json
  [{:keys [response] :as context}]
  (let [ch (chan 1)]
    (clojure.pprint/pprint context)
    (go
      (>! ch (assoc context :response (assoc response
                                             :status 201
                                             :body {}))))
    ch))
