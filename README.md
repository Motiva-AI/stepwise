[![CircleCI](https://circleci.com/gh/uwcpdx/stepwise/tree/master.svg?style=svg)](https://circleci.com/gh/uwcpdx/stepwise/tree/master) 

## Stepwise

Stepwise is an idiomatic Clojure library for [AWS Step Functions](https://aws.amazon.com/step-functions/). Use it to coordinate processes in distributed systems with AWS handling state, branching, and retries. Features:

 * Lightly sugared EDN representation of the [Amazon States Language](https://states-language.net/spec.html)
 * Activity task polling and handling ready for [component](https://github.com/stuartsierra/component) (or similar)
 * Tooling for [rapid development via code reloading](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)

Our production bioinformatics pipeline is implemented using this library.

### Basic Usage

Here's how to make a trivial state machine that just adds two inputs together. The only prerequisite is to install the [AWS CLI](https://aws.amazon.com/cli/) and run `aws configure` to set up your authentication and region.

For your dependencies:

`[uwcpdx/stepwise "0.5.3"]`

At the REPL:

```clojure
(require '[stepwise.core :as stepwise])

(stepwise/create-state-machine :adder
                               {:start-at :add
                                :states   {:add {:type     :task
                                                 :resource :activity/add
                                                 :end      true}}})
=> "arn:aws:states:us-west-2:XXXXXXXXXXXX:stateMachine:adder"

(stepwise/start-workers {:activity/add (fn [{:keys [x y]}] (+ x y))})
=> ...

(stepwise/run-execution :adder {:input {:x 1 :y 1}})
=>
{:input             {:x 1 :y 1}
 :output            2
 :state-machine-arn "arn:aws:states:us-west-2:XXXXXXXXXXXX:stateMachine:adder"
 :start-date        #inst"2017-06-20T22:48:14.241-00:00"
 :stop-date         #inst"2017-06-20T22:48:14.425-00:00"
 :arn               "arn:aws:states:us-west-2:XXXXXXXXXXXX:execution:adder:9c5623c6-eee3-49fa-a7d6-22e3ca236c9a"
 :status            "SUCCEEDED"
 :name              "9c5623c6-eee3-49fa-a7d6-22e3ca236c9a"}

(stepwise/shutdown-workers *2)
=> #{:done}
```

