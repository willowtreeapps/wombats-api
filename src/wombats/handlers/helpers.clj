(ns wombats.handlers.helpers
  (:require [wombats.constants :refer [errors]]))

(defn wombat-error
  "Throws an error that will be caught by the exception interceptor."
  [{code :code
    details :details
    params :params
    message :message
    field-error :field-error
    :or {code 1
         details {}
         params []
         message nil
         field-error nil}}]

  (let [message (or message
                    (get errors code "Oops, looks like something went wrong."))]
    (throw (ex-info "Wombat Error" (cond-> {:type :wombat-error
                                            :message (->> params
                                                          (into [message])
                                                          (apply format))
                                            :details details
                                            :code code}
                                     (not (nil? field-error))
                                     (merge {:field-error field-error}))))))
