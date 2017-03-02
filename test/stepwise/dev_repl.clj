(ns stepwise.dev-repl
  (:require [stepwise.model :as mdl]
            [stepwise.sugar :as sgr]
            [stepwise.core :as core]
            [clojure.spec.gen :as sgen]
            [clojure.repl :refer :all]
            [stepwise.specs.sugar :as sgrs]
            [alembic.still :refer [lein load-project]]
            [clojure.tools.namespace.repl :as ctn]
            [clojure.walk :as walk]
            [stepwise.specs.model]
            [stepwise.client :as client]
            [clojure.spec :as s]
            [clojure.test.check.generators :as gen]))

(defn go [])

(defn reset []
  (ctn/refresh :after 'stepwise.dev-repl/go))



(def state-machines
  {::analysis-run {:start-at :validate-input
                   :states   {:validate-input    {:type :task}
                              :describe-task-def {:type        :task
                                                  :result-path "$.task-def"
                                                  :next        :write-params-json}
                              :write-params-json {:type       :task
                                                  :input-path "$.input"
                                                  :next       :start-ecs-task}
                              :start-ecs-task    {:type :task
                                                  :next :describe-task}
                              :describe-task     {:type        :task
                                                  :result-path "$.task"}
                              }}})

(defn sandbox []
  #_(s/explain-data ::sgr/state-machine
                    {:start-at :my-choice
                     :states   {:my-choice {:type    :choice
                                            :choices [{:condition [:and
                                                                   [:= "$.my-string" "hello"]
                                                                   [:not "$.my-bool"]]
                                                       :next      :foo-the-bar}]}}})

  #_(-> {:start-at :my-choice
         :states   {:my-choice {:type               :choice
                                :default-state-name :ender
                                :choices            [{:condition [:and
                                                                  [:= "$.my-string" "hello"]
                                                                  [:not "$.my-bool"]]
                                                      :next      :ender}]}
                    :ender     {:type :pass
                                :end  true}}}
        (sgr/desugar)
        (sgr/sugar))
  ;(gen/sample (s/gen ::sgr/error))
  (lein test)
  #_(sgr/sugar-comparison :stepwise.model/numeric-eq #:stepwise.model{:variable "0", :expected-value-double 1.0})
  ;#spy/p (client/describe-state-machine "arn:aws:states:us-west-2:256212633204:stateMachine:hello-world")
  ;(client/delete-state-machine "arn:aws:states:us-west-2:256212633204:stateMachine:test-machine")
  ;(Thread/sleep 1000)
  ;(client/create-state-machine "test-machine3"
  ;                             {:start-at :foo
  ;                              :states   {:foo {:type    :wait
  ;                                               :seconds 600
  ;                                               :end     true}}}
  ;                             "arn:aws:iam::256212633204:role/service-role/StatesExecutionRole-us-west-2")
  ;(client/start-execution "arn:aws:states:us-west-2:256212633204:stateMachine:test-machine" {:input {:hi "foo"}})
  ;#spy/p (client/get-execution-history "arn:aws:states:us-west-2:256212633204:execution:test-machine:ebba36d3-3a4c-4d51-a97b-d6409043a998")
  ;(client/list-state-machines)
  #_(core/boot-workers "ncgl-dev-dacc"
                       {:hello-world-v3 {:start-at :foo
                                         :states   {:foo {:type     :task
                                                          :resource ::add
                                                          :end      true}}}}
                       {::add (fn [])})
  #_(let [namespace   "ncgl-dev-dacc"
        rand-ns     (str (rand-int 20))
        machine-id  (keyword "dev-repl" rand-ns)
        activity-kw (keyword "dev-repl" rand-ns)
        workers     (core/start-workers namespace {activity-kw (fn [{:keys [a b]}] (throw (ex-info "hi" {:error :blamo})))})]
    (core/create-state-machine namespace
                               machine-id
                               {:start-at :foo
                                :states   {:foo {:type            :task
                                                 :resource        activity-kw
                                                 :timeout-seconds 3
                                                 :end             true}}})

    #spy/p (core/run-execution namespace
                               machine-id
                               {:input {:a 1
                                        :b 2}})
    (core/shutdown-workers workers)
    ))

