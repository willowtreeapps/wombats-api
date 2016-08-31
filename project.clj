(defproject wombats-api "0.0.1"

  :description "Wombats"
  :url ""

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [selmer "1.0.7"]
                 [markdown-clj "0.9.89"]
                 [ring-middleware-format "0.7.0"]
                 [metosin/ring-http-response "0.8.0"]
                 [bouncer "1.0.0"]
                 [org.webjars/bootstrap "4.0.0-alpha.3"]
                 [org.webjars/font-awesome "4.6.3"]
                 [org.webjars.bower/tether "1.3.3"]
                 [org.webjars/jquery "3.0.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [compojure "1.5.1"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.2.1"]
                 [luminus/ring-ttl-session "0.3.1"]
                 [mount "0.1.10"]
                 [cprop "0.1.8"]
                 [org.clojure/tools.cli "0.3.5"]
                 [luminus-nrepl "0.1.4"]
                 [cider/cider-nrepl "0.14.0-SNAPSHOT"]
                 [com.novemberain/monger "3.0.0-rc2"]
                 [metosin/compojure-api "1.1.6"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [luminus-http-kit "0.1.4"]
                 [buddy "1.0.0"]
                 [base64-clj "0.1.1"]
                 [clojail "1.0.6"]]

  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main wombats-api.core

  :plugins [[lein-cprop "1.0.1"]
            [lein-kibit "0.1.2"]
            [lein-cloverage "1.0.6"]
            ;; TODO This is throwing an error around source-paths for tests
            [jonase/eastwood "0.2.3"]]

  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "wombats-api.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]

   :project/dev  {:dependencies [[prone "1.1.1"]
                                 [ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.5.0"]
                                 [pjstadig/humane-test-output "0.8.1"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.14.0"]]

                  :source-paths ["env/dev/clj" "test/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:resource-paths ["env/dev/resources" "env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
