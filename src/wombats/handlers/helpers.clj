(ns wombats.handlers.helpers)

(defn handler-error
  "Throws an error that will be caught by the exception interceptor."
  ([message]
   (handler-error message {}))
  ([message data]
   (throw (ex-info "Handler Error"
                   {:type :handler-error
                    :message message
                    :reason data}))))
