(ns wombats
  (:require [uswitch.lambada.core :refer [deflambdafn]]
            [cheshire.core :as cheshire]
            [clojure.java.io :as io]))

;; These are the variables we clear
(defonce envs ["AWS_ACCESS_KEY"
               "AWS_ACCESS_KEY_ID"
               "AWS_SECRET_ACCESS_KEY" 
               "AWS_SECRET_KEY"
               "AWS_SECURITY_TOKEN" 
               "AWS_SESSION_TOKEN"])

(defn- clear-envs
  "Clears environment variables so they're not accessible"
  []
  (let [env (System/getenv)
        cl (.getClass env)
        field (.getDeclaredField cl "m")]

    (.setAccessible field true)

    (let [writable-env (.get field env)]
      (doseq [env envs]
        (.put writable-env env "")))))

(defn handle-event
  [event time-left]
  ;; Call the passed in code with the appropriate args
  (try
    (let [user-defined-code (load-string (:code event))
          state (:state event)
          user-response (user-defined-code state time-left)]
      {:response user-response
       :error nil})
    (catch Exception e
      {:response nil
       :error {:message (.getMessage e)
               :stackTrace (map str (.getStackTrace e))}})))

(deflambdafn wombats.Handler
  [in out ctx]  
  (clear-envs)
  (let [time-left (fn [] (.getRemainingTimeInMillis ctx))
        event (cheshire/parse-stream (io/reader in) true)
        res (handle-event event time-left)]

    (with-open [w (io/writer out)]
      (cheshire/generate-stream res w))))
