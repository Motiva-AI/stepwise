(defproject uwcpdx/stepwise "0.3.0-SNAPSHOT"
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
  :repositories {"snapshots" {:url      "https://ncgl.jfrog.io/ncgl/libs-snapshot-local"
                              :username :env/artifactory_user
                              :password :env/artifactory_password}}
  :dependencies [[uwcpdx/bean-dip "0.7.1" :exclusions [joda-time]]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.3.441"]
                 [com.amazonaws/aws-java-sdk-iam "1.11.86"]
                 [com.amazonaws/aws-java-sdk-sts "1.11.86"]
                 [com.amazonaws/aws-java-sdk-stepfunctions "1.11.86"]])

