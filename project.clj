(defproject uwcpdx/stepwise "0.5.5"
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/clojure "1.9.0-alpha14"]
                                  [pjstadig/humane-test-output "0.8.1"]
                                  [org.slf4j/slf4j-simple "1.7.25"]
                                  [com.gfredericks/test.chuck "0.2.7"]
                                  [alembic "0.3.2"]
                                  [org.clojure/test.check "0.9.0"]]
                   :injections   [(require 'pjstadig.humane-test-output)
                                  (pjstadig.humane-test-output/activate!)]
                   :main         stepwise.dev-repl}}
  :deploy-repositories {"clojars" {:url           "https://clojars.org/repo"
                                   :username      :env/CLOJARS_USERNAME
                                   :password      :env/CLOJARS_PASSWORD
                                   :sign-releases false}}
  :dependencies [[uwcpdx/bean-dip "0.7.2" :exclusions [joda-time]]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.async "0.3.441"]
                 [com.amazonaws/aws-java-sdk-iam "1.11.86"]
                 [com.amazonaws/aws-java-sdk-sts "1.11.86"]
                 [com.amazonaws/aws-java-sdk-stepfunctions "1.11.86"]])

