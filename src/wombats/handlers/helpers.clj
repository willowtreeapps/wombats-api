(ns wombats.handlers.helpers
  (:require [wombats.constants :refer [errors]]))

(defn wombat-error
  "Throws an error that will be caught by the exception interceptor."
  [{code :code
    details :details
    params :params,
    :or {code 1
         details {}
         params []}}]

  (let [message (get errors code "Oops, looks like something went wrong.")]
    (throw (ex-info "Wombat Error" {:type :wombat-error
                                    :message (->> params
                                                  (into [message])
                                                  (apply format))
                                    :details details
                                    :code code}))))
