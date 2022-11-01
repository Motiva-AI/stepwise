(defproject ai.motiva/stepwise "0.9.0-SNAPSHOT"
  :description "Clojure AWS Step Functions library"
  :url "https://github.com/Motiva-AI/stepwise"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.3.0"]
                                  [org.clojure/clojure "1.11.1"]
                                  [org.clojure/spec.alpha "0.3.218"]
                                  [pjstadig/humane-test-output "0.11.0"]
                                  [org.slf4j/slf4j-simple "2.0.3"]
                                  [com.gfredericks/test.chuck "0.2.13"]
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
                 [com.amazonaws/aws-java-sdk-iam "1.12.332"]
                 [com.amazonaws/aws-java-sdk-sts "1.12.332"]
                 [com.amazonaws/aws-java-sdk-stepfunctions "1.12.332"]
                 [com.cognitect.aws/api "0.8.612"]
                 [com.cognitect.aws/endpoints "1.1.12.321"]
                 [com.cognitect.aws/s3 "822.2.1145.0"]
                 [com.taoensso/nippy "3.2.0"]])

