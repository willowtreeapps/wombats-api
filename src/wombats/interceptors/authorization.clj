(ns wombats.interceptors.authorization
  (:require [wombats.interceptors.current-user :refer [get-current-user]]))

(defn add-security-settings
  "Adds the security settings map to context"
  [security-settings]
  {:name ::security-settings
   :enter (fn [context] (assoc context ::security-settings security-settings))})

(defn authorization-error
  "Throws an error that will be caught by the exception interceptor."
  ([]
   (authorization-error "Unauthorized" {}))
  ([message]
   (authorization-error message {}))
  ([message data]
   (throw (ex-info "Unauthorized"
                   {:type :unauthorized
                    :message message
                    :reason data}))))

(defn get-hashing-secret
  [context]
  (get-in context [::security-settings :signing-secret]))

(defn add-api-settings
  [api-settings]
  {:name ::api-settings
   :enter (fn [context] (assoc context ::api-settings api-settings))})

(defn get-api-uri
  [context]
  (get-in context [::api-settings :uri]))

(defn- user-has-role?
  "Checks to see if a user has a role contained in the passed in role set"
  [user roles]
  (let [user-roles (set (map :db/ident (:user/roles user)))]
    (not (empty? (clojure.set/intersection user-roles roles)))))

#_(defn- get-permissions
  [context]
  (get context ::permissions #{}))

#_(def ^:private check-permissions
  {:name ::check-permissions
   :enter (fn [context]
            (let [authorized-roles (get-permissions context)
                  current-user (get-current-user context)
                  _ (when-not current-user
                      (authorization-error))]
              (if-not (user-has-role? current-user authorized-roles)
                (authorization-error)
                context)))})

#_(defn- ensure-check-permissions-interceptor
  [context]
  (enqueue context [check-permissions]))

#_(defn- add-permissions
  "Adds a set of permissions to a route"
  [context permissions]
  (assoc context ::permissions (clojure.set/union (get-permissions context)
                                                  permissions)))

#_(defn authorize
  "Protects a route (and it's children) from unauthorized access"
  [authorized-roles]
  {:name ::authorize-user
   :enter (fn [context]
            (-> context
                (ensure-check-permissions-interceptor)
                (add-permissions authorized-roles)))})

(defn authorize
  "Protects a route (and it's children) from unauthorized access"
  [authorized-roles]
  {:name ::authorize-user
   :enter (fn [context]
            (let [current-user (get-current-user context)
                  _ (when-not current-user
                      (authorization-error))]
              (if-not (user-has-role? current-user authorized-roles)
                (authorization-error)
                context)))})
