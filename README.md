[![CircleCI](https://circleci.com/gh/uwcpdx/stepwise/tree/master.svg?style=svg)](https://circleci.com/gh/uwcpdx/stepwise/tree/master) 

# Stepwise

Stepwise is an idiomatic Clojure library for [AWS Step Functions](https://aws.amazon.com/step-functions/). Use it to coordinate asynchronous, distributed processes with AWS managing state, branching, and retries.

Implemented Features:

 * Validated EDN representation of the [Amazon States Language](https://states-language.net/spec.html)
 * Activity task polling and handling ready for [component](https://github.com/stuartsierra/component), [integrant](https://github.com/weavejester/integrant), or similar.
 * Tooling for [rapid development via code reloading](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)

Future Features:

 * Transparent serverless deployment of activity task handlers as [AWS Lambda](https://aws.amazon.com/lambda/) functions (possibly via [portkey](https://github.com/cgrand/portkey))
 * Support for [test.check](https://github.com/clojure/test.check) validation of state machine definitions via emulation of the Step Functions backend

Our production bioinformatics pipeline is implemented using this library. Still pre-1.0 and some minor API changes are likely.

## Basic Usage

Here's how to make a trivial state machine that just adds two inputs together. The only prerequisite is to install the [AWS CLI](https://aws.amazon.com/cli/) and run `aws configure` to set up your authentication and region.

For your dependencies:

`[uwcpdx/stepwise "0.5.8"]`

At the REPL:

```clojure
(require '[stepwise.core :as stepwise])

(stepwise/ensure-state-machine :adder
                               {:start-at :add
                                :states   {:add {:type     :task
                                                 :resource :activity/add
                                                 :end      true}}})
=> "arn:aws:states:us-west-2:123456789012:stateMachine:adder"

(stepwise/start-workers {:activity/add (fn [{:keys [x y]}] (+ x y))})
=> ...

(stepwise/run-execution :adder {:input {:x 1 :y 1}})
=>
{:input             {:x 1 :y 1}
 :output            2
 :state-machine-arn "arn:aws:states:us-west-2:123456789012:stateMachine:adder"
 :start-date        #inst"2017-06-20T22:48:14.241-00:00"
 :stop-date         #inst"2017-06-20T22:48:14.425-00:00"
 :arn               "arn:aws:states:us-west-2:123456789012:execution:adder:9c5623c6-eee3-49fa-a7d6-22e3ca236c9a"
 :status            "SUCCEEDED"
 :name              "9c5623c6-eee3-49fa-a7d6-22e3ca236c9a"}

(stepwise/shutdown-workers *2)
=> #{:done}
```

## Development Workflow

Stepwise enables a rapidly cycling development workflow for Step Functions. State machine definition updates are eventually consistent, so new state machines need to be created on each cycle during development. Also, activity task polls are long and cannot be interrupted, demanding registration of new activities (or a JVM restart) to prevent stealing by stale bytecode. Stepwise provides a single function, `stepwise.reloaded/run-execution`, that uses fresh state machine and activity task registrations to run a state machine execution wherein code changes are immediately reflected.

Example:

```clojure
(stepwise.reloaded/run-execution :adder
                                 {:start-at :add
                                  :states   {:add {:type     :task
                                                   :resource :activity/add
                                                   :end      true}}}
                                 {:activity/add (fn [{:keys [x y]}] (+ x y))}
                                 {:x 41 :y 1})
=>
{:output 42,
 :state-machine-arn "arn:aws:states:us-west-2:256212633204:stateMachine:adder-1522697821734",
 :start-date #inst"2018-04-02T19:37:02.061-00:00",
 :stop-date #inst"2018-04-02T19:37:02.183-00:00",
 :input {:x 41,
         :y 1,
         :state-machine-name "adder-1522697821734",
         :execution-name "93f1d268-b2ff-4261-bf53-8ff92d7bc2c2"},
 :arn "arn:aws:states:us-west-2:256212633204:execution:adder-1522697821734:93f1d268-b2ff-4261-bf53-8ff92d7bc2c2",
 :status "SUCCEEDED",
 :name "93f1d268-b2ff-4261-bf53-8ff92d7bc2c2"}
```

Naturally your state machine and handlers will not be defined inline like this, so pair this call with something like [tools.namespace](https://github.com/clojure/tools.namespace) or [Cursive](https://cursive-ide.com/)'s native code reloading to rapidly try out changes to your namespaces.

## EDN States Language Representation

Stepwise represents the [Amazon States Language](https://states-language.net/spec.html) as EDN.

The only pervasive departure from the JSON representation is the use of lowered-hyphen keywords for keys, state names, and state types instead of CamelCase strings. The official AWS SDK validates your state machine definitions, and its exceptions are let through unmolested, so their messages are in terms of the CamelCase naming. When retrieving definitions from AWS they are translated into the EDN representation whether created with stepwise or not (state names are converted to keywords but otherwise untouched).

Aside from keywords being substituted for strings, the other sugaring is on:

 * Condition expressions in [choice states](https://states-language.net/spec.html#choice-state)
 * Error matching expressions in [retry](https://states-language.net/spec.html#retrying-after-error) and [catch](fallback-states) blocks
 * Activity task resources

### Condition Expressions

Condition expressions in choice states are sugared in stepwise as illustrated by the example below:

```
JSON
----
{
    "Not": {
      "Variable": "$.type",
      "StringEquals": "Private"
    },
    "Next": "Public"
}

EDN
---
{:condition [:not [:= "$.type" "Private"]]
 :next      :public}

JSON
----
{
  "And": [
    {
      "Variable": "$.value",
      "NumericGreaterThanEquals": 20
    },
    {
      "Variable": "$.value",
      "NumericLessThan": 30
    }
  ],
  "Next": "ValueInTwenties"
}

EDN
---
{:condition [:and [>= "$.value" 20]
                  [< "$.value" 30]]
 :next      :value-in-twenties}
```

See [src/stepwise/sugar.clj](src/stepwise/sugar.clj) and [src/stepwise/specs/sugar.clj](src/stepwise/specs/sugar.clj) for full details.

### Error Matching

In stepwise the `:error-equals` key that corresponds to the `ErrorEquals` key in the states language can be a keyword or collection of keywords:

```
JSON
----
"Retry" : [
    {
      "ErrorEquals": [ "States.ALL" ]
    }
]

EDN
---
{:retry [{:error-equals :States.ALL}]}
```

You can of course use keywords for your custom error names, including namespaced keywords.

### Activity Task Resources

Activity task resources can be specified as keywords and `stepwise.core/ensure-state-machine` will register appropriately named activities for you and substitute in their ARNs. For example:

```
JSON
----
{
  "StartAt": "Add",
  "States": {
    "Add": {
      "Type": "Task",
      "Resource": "arn:aws:states:us-west-2:123456789012:activity:add",
      "End": true
    }
  }
}

EDN
---
{:start-at :add
 :states   {:add {:type     :task
                  :resource :add
                  :end      true}}}
```

You can also supply an ARN string for the resource to specify a lambda task or activity task managed outside of stepwise.

