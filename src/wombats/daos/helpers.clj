(ns wombats.daos.helpers
  (:require [datomic.api :as d]))

(defn get-fn
  "Helper function used to pull the correct dao out of context"
  [fn-key context]
  (let [dao-fn (fn-key (:wombats.interceptors.dao/daos context))]
    (if dao-fn
      dao-fn
      (throw (Exception. (str "Dao function " fn-key " was not found in the dao map"))))))

(defn gen-id
  "Generates a uuid string to use with db entities"
  []
  (str (java.util.UUID/randomUUID)))

(defn get-entity-by-prop
  "Helper function for pulling an entity out of datomic.

  It seems that if [*] is passed to pull and the value you are
  searching for the result will be '{:db/id nil}'. This helper
  will normalize all results to 'nil' if the record is not found."
  ([conn prop value]
   (get-entity-by-prop conn prop value '[*]))
  ([conn prop value display-props]
   (let [result (d/pull (d/db conn) display-props [prop value])
         no-result (or (= result nil)
                       (and (= display-props '[*])
                            (= nil (:db/id result))))]
     (if no-result nil result))))

(defn get-entities-by-prop
  "Returns a collection of entites that contain a given prop"
  ([conn prop]
   (get-entities-by-prop conn prop '[*]))
  ([conn prop display-props]
   (let [db (d/db conn)
         eids (apply concat
                     (d/q '[:find ?e
                            :in $ ?entity-prop
                            :where [?e ?entity-prop]]
                          db
                          prop))]
     (remove nil?
             (d/pull-many db
                          display-props
                          eids)))))

(defn get-entity-id
  "Returns the entity id associated with a given unique prop / value"
  [conn prop value]
  (ffirst
   (d/q '[:find ?e
          :in $ ?prop-name ?prop-value
          :where [?e ?prop-name ?prop-value]]
        (d/db conn)
        prop
        value)))

(defn retract-entity-by-prop
  ([conn prop value]
   (retract-entity-by-prop conn prop value "Entity removed"))
  ([conn prop value msg]
   (let [entity-id (get-entity-id conn prop value)]
     (future
       (if entity-id
         (do
           @(d/transact-async conn [[:db.fn/retractEntity entity-id]])
           msg)
         msg)))))

(defn db-requirement-error
  "Throws an error that will be caught by the exception interceptor."
  ([message]
   (db-requirement-error message {}))
  ([message data]
   (throw (ex-info "Datomic Requirement Error"
                   {:type :db-requirement-error
                    :message message
                    :reason data}))))
