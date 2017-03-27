(ns wombats.game.dev-mode.core
  (:require [cheshire.core :as cheshire]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clojure.core.async :as async]
            [clojure.string :refer [split]]
            [wombats.game.dev-mode.clojure :as clj]
            [wombats.game.dev-mode.javascript :as js]
            [wombats.game.dev-mode.python :as py]))

(def ^:const dev-mode-handlers
  {:clj clj/handler
   :js js/handler
   :py py/handler})

(def ^:dynamic *timeout-in* 2000)

(defn request-handler
  [{:keys [code path path-ext]} request-body]
  (let [c (async/chan)
        timeout (tc/to-long (t/plus (t/now) (t/millis *timeout-in*)))
        handler (dev-mode-handlers path-ext)]

    (async/go
      (async/>! c (try
                    (str "{\"response\": "
                         (handler code request-body timeout)
                         ", \"error\": null}")
                    (catch Exception e
                      (let [msg (.getMessage e)
                            st (.getStackTrace e)]
                        (cheshire/generate-string
                         {:response nil
                          :error {:message msg
                                  :stackTrace (map str st)}}))))))
      (first (async/alts!! [c (async/timeout *timeout-in*)]))))
