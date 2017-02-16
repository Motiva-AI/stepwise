(defproject stepwise "0.1.0-SNAPSHOT"
  :profiles {:dev {:dependencies [[spyscope "0.1.7-SNAPSHOT"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/clojure "1.9.0-alpha14"]
                                  [pjstadig/humane-test-output "0.8.1"]
                                  [com.gfredericks/test.chuck "0.2.7"]
                                  [alembic "0.3.2"]
                                  [org.clojure/test.check "0.9.0"]]
                   :injections   [(require 'pjstadig.humane-test-output)
                                  (require 'spyscope.core)
                                  (pjstadig.humane-test-output/activate!)]
                   :main         stepwise.dev-repl}}
  :dependencies [[uwcpdx/bean-dip "0.6.0-SNAPSHOT"]
                 [com.amazonaws/aws-java-sdk-stepfunctions "1.11.86"]])

