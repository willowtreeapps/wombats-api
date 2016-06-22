(ns battlebots.services.github
  (:require [org.httpkit.client :as http]
            [base64-clj.core :as b64]
            [cheshire.core :refer [parse-string]]))

(defn decode-bot
  [content]
  (let [base64-string (clojure.string/replace content #"\n" "")]
    (b64/decode base64-string "UTF-8")))

(defn get-bot-code
  [access-token contents-url]
  (let [{:keys [status body] :as res} @(http/get contents-url {:headers {"Authorization" (str "token " access-token)
                                                                         "Accept" "application/json"}})]
    (if (= status 200)
      (decode-bot (:content (parse-string body true)))
      nil)))
