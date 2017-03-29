(ns wombats.test.handlers.helpers
  (:require [clojure.test :refer :all]
            [wombats.handlers.helpers :as h]))

(deftest format-url
  (testing "properly formats a url"
    (is (= "http://www.google.com/api/v1?test=pass"
           (h/format-url "http://www.google.com"
                         "/api/v1"
                         {:test "pass"}))))
  (testing "url encoding support"
    (is (= "http://www.google.com/api/v1?test=pass%20with%20flying%20colors"
           (h/format-url "http://www.google.com"
                         "/api/v1"
                         {:test "pass with flying colors"}))))
  (testing "multi value query params are supported"
    (is (= "http://www.google.com/api/v1?test=pass&value=one&value=two"
           (h/format-url "http://www.google.com"
                         "/api/v1"
                         {:test "pass"
                          :value ["one" "two"]})))))
