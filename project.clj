(defproject stepwise "0.1.0-SNAPSHOT"
  :profiles {:dev {:dependencies [[spyscope "0.1.7-SNAPSHOT"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/test.check "0.9.0"]]
                   :injections   [(require 'spyscope.core)]
                   :main         stepwise.dev-repl}}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [uwcpdx/bean-dip "0.6.0-SNAPSHOT"]
                 [com.amazonaws/aws-java-sdk-stepfunctions "1.11.86"]])

