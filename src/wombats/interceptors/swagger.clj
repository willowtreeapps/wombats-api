(ns wombats.interceptors.swagger
  (:require [io.pedestal.interceptor.helpers :refer [defbefore]]
            [clojure.core.async :refer [chan go >!]]))

(def annotation
  "Gets documentation from an annotated object"
  (comp ::doc meta))
