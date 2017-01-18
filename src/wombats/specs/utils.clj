(ns wombats.specs.utils
  (:require [clojure.spec :as s]))

(defn validate-input
  ([spec input]
   (validate-input spec input "Invalid input"))
  ([spec input err-msg]
   (if-not (s/valid? spec input)
     (let [reason (s/explain-data spec input)]
       (throw (ex-info "Invalid Input"
                       {:type :invalid-schema
                        :message err-msg
                        :reason (:clojure.spec/problems reason)}))))))
