(defproject ai.motiva/stepwise "0.8.2-SNAPSHOT"
  :description "Clojure AWS Step Functions library"
  :url "https://github.com/Motiva-AI/stepwise"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.4.4"]
                                  [org.clojure/clojure "1.11.1"]
                                  [org.clojure/spec.alpha "0.3.218"]
                                  [pjstadig/humane-test-output "0.11.0"]
                                  [org.slf4j/slf4j-simple "2.0.7"]
                                  [com.gfredericks/test.chuck "0.2.14"]
                                  [circleci/bond "0.6.0"]
                                  ; TODO pending jdk 9 compat https://github.com/pallet/alembic/pull/16
                                  #_[alembic "0.3.2"]
                                  [org.clojure/test.check "1.1.1"]]
                   :injections   [(require 'pjstadig.humane-test-output)
                                  (pjstadig.humane-test-output/activate!)]
                   :main         stepwise.dev-repl}}

  :deploy-repositories {"clojars" {:url           "https://clojars.org/repo"
                                   :username      "motiva-ai"
                                   :password      :env
                                   :sign-releases false}}

  :dependencies [[uwcpdx/bean-dip "0.7.6"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/core.async "1.6.673"]
                 [metosin/sieppari "0.0.0-alpha13"]
                 [com.amazonaws/aws-java-sdk-iam "1.12.508"]
                 [com.amazonaws/aws-java-sdk-sts "1.12.508"]
                 [com.amazonaws/aws-java-sdk-stepfunctions "1.12.508"]
                 [com.cognitect.aws/api "0.8.686"]
                 [com.cognitect.aws/endpoints "1.1.12.504"]
                 [com.cognitect.aws/s3 "848.2.1413.0"]
                 [com.taoensso/nippy "3.2.0"]])

