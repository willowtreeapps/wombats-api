(ns wombats.interceptors.authorization)

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
