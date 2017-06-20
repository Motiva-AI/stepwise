## Stepwise

Stepwise is an idiomatic Clojure library for [AWS Step Functions](https://aws.amazon.com/step-functions/). It enables you to easily develop coordination workflows for distributed systems using a minimal, data-centric API.

 * Lightly sugared EDN representation of the [Amazon States Language](https://states-language.net/spec.html)
 * Activity task polling and handling
 * Tooling for [rapid development via code reloading](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)

### Example


```clojure
(require '[stepwise.core :as stepwise])

(stepwise/create-state-machine :adder
                               {:start-at :add
                                :states   {:add {:type     :task
                                                 :resource :activity/add
                                                 :end      true}}})
=> "arn:aws:states:us-west-2:256212633204:stateMachine:adder"

(def workers (stepwise/start-workers {:activity/add (fn [{:keys [x y]}] (+ x y))}))
=> #'stepwise.dev-repl/workers

(:output (stepwise/run-execution :adder {:input {:x 1 :y 1}}))
=> 2

(stepwise/shutdown-workers workers)
=> #{:done}
```
