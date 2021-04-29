(defproject motiva/stepwise "0.6.0-SNAPSHOT"
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.1.0"]
                                  [org.clojure/clojure "1.10.3"]
                                  [org.clojure/spec.alpha "0.2.194"]
                                  [pjstadig/humane-test-output "0.11.0"]
                                  [org.slf4j/slf4j-simple "1.7.30"]
                                  [com.gfredericks/test.chuck "0.2.10"]
                                  [org.clojure/clojure "1.9.0"]
                                  ; TODO pending jdk 9 compat https://github.com/pallet/alembic/pull/16
                                  #_[alembic "0.3.2"]
                                  [org.clojure/test.check "1.1.0"]]
                   :injections   [(require 'pjstadig.humane-test-output)
                                  (pjstadig.humane-test-output/activate!)]
                   :main         stepwise.dev-repl}}

  :deploy-repositories {"clojars" {:url           "https://clojars.org/repo"
                                   :username      "motiva-ai"
                                   :password      :env
                                   :sign-releases false}}

  :dependencies [[uwcpdx/bean-dip "0.7.6"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.clojure/data.json "2.2.2"]
                 [org.clojure/core.async "1.3.618"]
                 [com.amazonaws/aws-java-sdk-iam "1.11.1007"]
                 [com.amazonaws/aws-java-sdk-sts "1.11.1007"]
                 [com.amazonaws/aws-java-sdk-stepfunctions "1.11.1007"]])

