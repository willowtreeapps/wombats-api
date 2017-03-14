(ns wombats.handlers.access-key
  (:require [io.pedestal.interceptor.helpers :as interceptor]
            [clojure.spec :as s]
            [clj-time.core :as t]
            [wombats.specs.utils :as sutils]))

(def ^:private add-access-key-body-sample
  #:access-key{:key "wt2017_alpha"
               :max-number-of-uses 100
               :expiration-date (str (t/plus (t/now)
                                             (t/months 1)))
               :description "Initial alpha key for WillowTree internal use only"})

(def ^:private add-access-key-body-params
  {:name "access-key-body"
   :in "body"
   :description "values for a access key"
   :required true
   :default (str add-access-key-body-sample)
   :schema {}})

(def ^:swagger-spec get-access-keys-spec
  {"/api/v1/access_keys"
   {:get {:description "Returns a collection of all access keys in the system"
          :tags ["access-key"]
          :operationId "get-access-keys"
          :responses {:200 {:description "get-access-keys response"}}}}})

(def get-access-keys
  (interceptor/before
   ::get-access-keys
   (fn [{:keys [response] :as context}]
     (assoc context :response (assoc response
                                     :status 200
                                     :body [])))))

(s/def :access-key/id string?)
(s/def :access-key/key string?)
(s/def :access-key/max-number-of-uses #(instance? Long %))
(s/def :access-key/number-of-uses #(instance? Long %))
(s/def :access-key/expiration-date inst?)
(s/def :access-key/description string?)

(s/def ::new-access-key (s/keys :req [:access-key/key
                                      :access-key/max-number-of-uses
                                      :access-key/expiration-date
                                      :access-key/description]))

(def ^:swagger-spec add-access-key-spec
  {"/api/v1/access_keys"
   {:post {:description "Creates and returns a new access key"
           :tags ["access-key"]
           :operationId "add-access-keys"
           :parameters [add-access-key-body-params]
           :responses {:200 {:description "add-access-key response"}}}}})

(defn- format-new-access-key-request
  [access-key]
  (-> access-key
      (update :access-key/expiration-date #(when % (read-string (str "#inst \"" % "\""))))))

(def add-access-key
  (interceptor/before
   ::add-access-key
   (fn [{:keys [request response] :as context}]
     (let [access-key (->> (:edn-params request)
                           (format-new-access-key)
                           (sutils/validate-input ::new-access-key)
                           (add-access-key-fields))]

       (assoc context :response (assoc response
                                       :status 200
                                       :body access-key))))))
