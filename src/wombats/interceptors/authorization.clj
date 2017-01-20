(ns wombats.interceptors.authorization)

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
