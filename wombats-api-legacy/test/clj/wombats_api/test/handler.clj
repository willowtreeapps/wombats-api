(ns wombats-api.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [wombats-api.handler :refer :all]))

(deftest test-app
  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))
