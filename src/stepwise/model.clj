(ns stepwise.model
  (:require [bean-dip.core :as bd]
            [stepwise.serialization :as ser]
            [clojure.string :as strs])
  (:import (com.amazonaws.services.stepfunctions.model CreateActivityRequest
                                                       CreateActivityResult
                                                       CreateStateMachineRequest
                                                       CreateStateMachineResult
                                                       DeleteActivityRequest
                                                       DeleteStateMachineRequest
                                                       DescribeActivityRequest
                                                       DescribeActivityResult
                                                       DescribeExecutionRequest
                                                       DescribeExecutionResult
                                                       DescribeStateMachineRequest
                                                       DescribeStateMachineResult
                                                       GetActivityTaskRequest
                                                       GetActivityTaskResult
                                                       GetExecutionHistoryRequest
                                                       GetExecutionHistoryResult
                                                       HistoryEvent
                                                       ActivityFailedEventDetails
                                                       ActivityScheduleFailedEventDetails
                                                       ActivityScheduledEventDetails
                                                       ActivityStartedEventDetails
                                                       ActivitySucceededEventDetails
                                                       ActivityTimedOutEventDetails
                                                       ExecutionFailedEventDetails
                                                       ExecutionStartedEventDetails
                                                       ExecutionSucceededEventDetails
                                                       ExecutionAbortedEventDetails
                                                       ExecutionTimedOutEventDetails
                                                       StateEnteredEventDetails
                                                       StateExitedEventDetails
                                                       ListActivitiesRequest
                                                       ListActivitiesResult
                                                       ActivityListItem
                                                       ListExecutionsRequest
                                                       ListExecutionsResult
                                                       ExecutionListItem
                                                       ListStateMachinesRequest
                                                       ListStateMachinesResult
                                                       StateMachineListItem
                                                       SendTaskFailureRequest
                                                       SendTaskHeartbeatRequest
                                                       StartExecutionRequest
                                                       StopExecutionRequest
                                                       StartExecutionResult
                                                       SendTaskSuccessRequest)
           (com.amazonaws.services.stepfunctions.builder StateMachine StateMachine$Builder)
           (com.amazonaws.services.stepfunctions.builder.states ChoiceState FailState ParallelState
                                                                PassState SucceedState TaskState
                                                                WaitState Choice EndTransition
                                                                NextStateTransition Branch Catcher
                                                                Retrier WaitForSeconds
                                                                WaitForTimestampPath
                                                                WaitForSecondsPath
                                                                WaitForTimestamp$Builder
                                                                Branch$Builder
                                                                ParallelState$Builder
                                                                Transition$Builder
                                                                TaskState$Builder
                                                                Catcher$Builder Retrier$Builder
                                                                ChoiceState$Builder
                                                                PassState$Builder
                                                                WaitState$Builder
                                                                WaitForTimestamp
                                                                WaitForSecondsPath$Builder
                                                                WaitForTimestampPath$Builder
                                                                WaitForSeconds$Builder
                                                                NextStateTransition$Builder
                                                                EndTransition$Builder
                                                                Choice$Builder
                                                                FailState$Builder
                                                                SucceedState$Builder)
           (com.amazonaws.services.stepfunctions.builder.conditions AndCondition
                                                                    BooleanEqualsCondition
                                                                    NotCondition
                                                                    NumericEqualsCondition
                                                                    NumericGreaterThanCondition
                                                                    NumericGreaterThanOrEqualCondition
                                                                    NumericLessThanCondition
                                                                    OrCondition
                                                                    NumericLessThanOrEqualCondition
                                                                    StringEqualsCondition
                                                                    StringGreaterThanCondition
                                                                    StringGreaterThanOrEqualCondition
                                                                    StringLessThanCondition
                                                                    StringLessThanOrEqualCondition
                                                                    TimestampEqualsCondition
                                                                    TimestampLessThanOrEqualCondition
                                                                    TimestampLessThanCondition
                                                                    TimestampGreaterThanOrEqualCondition
                                                                    TimestampGreaterThanCondition
                                                                    AndCondition$Builder
                                                                    OrCondition$Builder
                                                                    TimestampLessThanOrEqualCondition$Builder
                                                                    TimestampLessThanCondition$Builder
                                                                    TimestampGreaterThanOrEqualCondition$Builder
                                                                    TimestampGreaterThanCondition$Builder
                                                                    TimestampEqualsCondition$Builder
                                                                    StringLessThanOrEqualCondition$Builder
                                                                    StringLessThanCondition$Builder
                                                                    StringGreaterThanOrEqualCondition$Builder
                                                                    StringGreaterThanCondition$Builder
                                                                    StringEqualsCondition$Builder
                                                                    NumericLessThanOrEqualCondition$Builder
                                                                    NumericLessThanCondition$Builder
                                                                    NumericGreaterThanOrEqualCondition$Builder
                                                                    NumericGreaterThanCondition$Builder
                                                                    NumericEqualsCondition$Builder
                                                                    BooleanEqualsCondition$Builder
                                                                    NotCondition$Builder)
           (java.util Date)))

(set! *warn-on-reflection* true)

(defmacro def-builder-translation [bean-class-sym field-specs & get-only-fields]
  (let [builder-form      (list (symbol (str bean-class-sym) "builder"))
        builder-class-sym (symbol (str bean-class-sym "$Builder"))]
    `(bd/def-builder-translation ~bean-class-sym
                                 ~builder-class-sym
                                 ~field-specs
                                 {:builder-form    ~builder-form
                                  :get-only-fields ~get-only-fields})))

(def-builder-translation WaitForTimestamp #{::timestamp})
(def-builder-translation WaitForSecondsPath #{::seconds-path})
(def-builder-translation WaitForTimestampPath #{::timestamp-path})
(def-builder-translation WaitForSeconds #{::seconds})

(def wait-kw->map->Bean
  {::timestamp      map->WaitForTimestamp$Builder
   ::seconds-path   map->WaitForSecondsPath$Builder
   ::timestamp-path map->WaitForTimestampPath$Builder
   ::seconds        map->WaitForSeconds$Builder})

(defmethod bd/->bean-val ::wait-for [_ wait-for]
  ((wait-kw->map->Bean (-> wait-for first key)) wait-for))

(defmethod bd/builder-override [Retrier ::error-equals] [_ ^Retrier$Builder builder error-equals]
  (.errorEquals builder (into-array String (map name error-equals))))

(defmethod bd/->bean-val ::backoff-rate [_ backoff-rate]
  (double backoff-rate))

(defmethod bd/->bean-val ::interval-seconds [_ interval-seconds]
  (int interval-seconds))

(defmethod bd/->bean-val ::max-attempts [_ max-attempts]
  (int max-attempts))

(def-builder-translation Retrier #{::backoff-rate ::error-equals ::interval-seconds ::max-attempts})

(def-builder-translation NextStateTransition #{::next-state-name ::terminal?} ::terminal?)

(def-builder-translation EndTransition #{::terminal?} ::terminal?)

(defn map->Transition ^Transition$Builder [transition]
  (if (= transition ::end)
    (map->EndTransition$Builder {})
    (map->NextStateTransition$Builder {::next-state-name transition})))

(defmethod bd/->bean-val ::transition [_ transition]
  (map->Transition transition))

(defmethod bd/builder-override [Catcher ::error-equals] [_ ^Catcher$Builder builder error-equals]
  (.errorEquals builder (into-array String (map name error-equals))))

(def-builder-translation Catcher #{::error-equals ::result-path ::transition})

(defmethod bd/->map-val ::error-equals [_ error-equals]
  (into #{} error-equals))

(defmethod bd/->map-val ::transition [_ transition]
  (condp instance? transition
    NextStateTransition (.getNextStateName ^NextStateTransition transition)
    EndTransition ::end))

(defmacro def-timestamp-cond [bean-class]
  `(def-builder-translation ~bean-class
                            #{[::variable String]
                              [:expected-value ::expected-value-timestamp Date]}))

(def-timestamp-cond TimestampLessThanOrEqualCondition)
(def-timestamp-cond TimestampLessThanCondition)
(def-timestamp-cond TimestampGreaterThanOrEqualCondition)
(def-timestamp-cond TimestampGreaterThanCondition)
(def-timestamp-cond TimestampEqualsCondition)

(defmacro def-string-cond [bean-class]
  `(def-builder-translation ~bean-class
                            #{[::variable String]
                              [:expected-value ::expected-value-string String]}))

(def-string-cond StringLessThanOrEqualCondition)
(def-string-cond StringLessThanCondition)
(def-string-cond StringGreaterThanOrEqualCondition)
(def-string-cond StringGreaterThanCondition)
(def-string-cond StringEqualsCondition)

(defmacro def-numeric-cond [bean-class]
  `(def-builder-translation ~bean-class
                            #{[::variable String]
                              [:expected-value ::expected-value-double Double]
                              [:expected-value ::expected-value-long Long]}))

(def-numeric-cond NumericLessThanOrEqualCondition)
(def-numeric-cond NumericLessThanCondition)
(def-numeric-cond NumericGreaterThanOrEqualCondition)
(def-numeric-cond NumericGreaterThanCondition)
(def-numeric-cond NumericEqualsCondition)

(def-builder-translation BooleanEqualsCondition
                         #{[::variable String]
                           [:expected-value ::expected-value-boolean Boolean]})

(declare tuple->Condition)

(defmethod bd/builder-override [AndCondition ::conditions] [_ ^AndCondition$Builder builder conditions]
  (doseq [condition conditions]
    (.condition builder (tuple->Condition condition))))

(def-builder-translation AndCondition #{::conditions})

(defmethod bd/builder-override [OrCondition ::conditions] [_ ^OrCondition$Builder builder conditions]
  (doseq [condition conditions]
    (.condition builder (tuple->Condition condition))))

(def-builder-translation OrCondition #{::conditions})
(def-builder-translation NotCondition #{::condition})

(def condition-kw->map->Bean
  {::and         map->AndCondition$Builder
   ::or          map->OrCondition$Builder
   ::not         map->NotCondition$Builder
   ::bool-eq     map->BooleanEqualsCondition$Builder
   ::numeric-eq  map->NumericEqualsCondition$Builder
   ::numeric-gt  map->NumericGreaterThanCondition$Builder
   ::numeric-gte map->NumericGreaterThanOrEqualCondition$Builder
   ::numeric-lt  map->NumericLessThanCondition$Builder
   ::numeric-lte map->NumericLessThanOrEqualCondition$Builder
   ::str-eq      map->StringEqualsCondition$Builder
   ::str-gt      map->StringGreaterThanCondition$Builder
   ::str-gte     map->StringGreaterThanOrEqualCondition$Builder
   ::str-lt      map->StringLessThanCondition$Builder
   ::str-lte     map->StringLessThanOrEqualCondition$Builder
   ::ts-eq       map->TimestampEqualsCondition$Builder
   ::ts-gt       map->TimestampGreaterThanCondition$Builder
   ::ts-gte      map->TimestampGreaterThanOrEqualCondition$Builder
   ::ts-lt       map->TimestampLessThanCondition$Builder
   ::ts-lte      map->TimestampLessThanOrEqualCondition$Builder})

(defn tuple->Condition [[condition attrs]]
  ((condition-kw->map->Bean condition) attrs))

(defmethod bd/->bean-val ::condition [_ condition]
  (tuple->Condition condition))

(def bean-class->condition-kw
  {AndCondition                         ::and
   OrCondition                          ::or
   NotCondition                         ::not
   BooleanEqualsCondition               ::bool-eq
   NumericEqualsCondition               ::numeric-eq
   NumericGreaterThanCondition          ::numeric-gt
   NumericGreaterThanOrEqualCondition   ::numeric-gte
   NumericLessThanCondition             ::numeric-lt
   NumericLessThanOrEqualCondition      ::numeric-lte
   StringEqualsCondition                ::str-eq
   StringGreaterThanCondition           ::str-gt
   StringGreaterThanOrEqualCondition    ::str-gte
   StringLessThanCondition              ::str-lt
   StringLessThanOrEqualCondition       ::str-lte
   TimestampEqualsCondition             ::ts-eq
   TimestampGreaterThanCondition        ::ts-gt
   TimestampGreaterThanOrEqualCondition ::ts-gte
   TimestampLessThanCondition           ::ts-lt
   TimestampLessThanOrEqualCondition    ::ts-lte})

(defn Condition->map [condition]
  [(bean-class->condition-kw (class condition))
   (bd/bean->map condition)])

(defmethod bd/->map-val ::conditions [_ conditions]
  (mapv Condition->map conditions))

(def-builder-translation Choice #{::condition ::transition})

(defmethod bd/->map-val ::condition [_ condition]
  (Condition->map condition))

(defmethod bd/builder-override [ChoiceState ::choices] [_ ^ChoiceState$Builder builder choices]
  (doseq [choice choices]
    (.choice builder (map->Choice$Builder choice))))

(def-builder-translation ChoiceState
                         #{::choices ::comment ::default-state-name ::input-path ::output-path
                           ::terminal-state?}
                         ::terminal-state?)

(def-builder-translation FailState #{::cause ::comment ::error})


(defmethod bd/builder-override [PassState ::transition] [_ ^PassState$Builder builder transition]
  (.transition builder (map->Transition transition)))

(def-builder-translation PassState
                         #{::comment ::input-path ::output-path [::result String] ::result-path
                           ::transition})

(def-builder-translation SucceedState
                         #{::comment ::input-path ::output-path ::terminal-state?}
                         ::terminal-state?)

(defmethod bd/builder-override [TaskState ::transition] [_ ^TaskState$Builder builder transition]
  (.transition builder (map->Transition transition)))

(defmethod bd/builder-override [TaskState ::catchers] [_ ^TaskState$Builder builder catchers]
  (doseq [catcher catchers]
    (.catcher builder (map->Catcher$Builder catcher))))

(defmethod bd/builder-override [TaskState ::retriers] [_ ^TaskState$Builder builder retriers]
  (doseq [retrier retriers]
    (.retrier builder (map->Retrier$Builder retrier))))

(defmethod bd/->bean-val ::timeout-seconds [_ timeout-seconds]
  (int timeout-seconds))

(defmethod bd/->bean-val ::heartbeat-seconds [_ heartbeat-seconds]
  (int heartbeat-seconds))

(def-builder-translation TaskState
                         #{::catchers ::comment ::heartbeat-seconds ::input-path ::output-path
                           ::resource ::result-path ::retriers ::timeout-seconds ::transition})

(defmethod bd/builder-override [WaitState ::transition] [_ ^WaitState$Builder builder transition]
  (.transition builder (map->Transition transition)))

(def-builder-translation WaitState
                         #{::comment ::input-path ::output-path ::transition ::wait-for})

(declare state-kw->map->Bean)

(defn states->bean-map [states]
  (into {}
        (map (fn [[name {:keys [::state-type] :as attrs}]]
               [name ((state-kw->map->Bean state-type) attrs)]))
        states))

(defmethod bd/builder-override [Branch ::states] [_ ^Branch$Builder builder states]
  (doseq [[name state] (states->bean-map states)]
    (.state builder name state)))

(def-builder-translation Branch #{::comment ::start-at ::states})

(defmethod bd/builder-override [ParallelState ::branches] [_ ^ParallelState$Builder builder branches]
  (doseq [branch branches]
    (.branch builder (map->Branch$Builder branch))))

(defmethod bd/builder-override [ParallelState ::catchers] [_ ^ParallelState$Builder builder catchers]
  (doseq [catcher catchers]
    (.catcher builder (map->Catcher$Builder catcher))))

(defmethod bd/builder-override [ParallelState ::retriers] [_ ^ParallelState$Builder builder retriers]
  (doseq [retrier retriers]
    (.retrier builder (map->Retrier$Builder retrier))))

(defmethod bd/builder-override [ParallelState ::transition] [_ ^ParallelState$Builder builder transition]
  (.transition builder (map->Transition transition)))

(def-builder-translation ParallelState
                         #{::branches ::catchers ::comment ::input-path ::output-path ::result-path
                           ::retriers ::transition})

(def state-kw->map->Bean
  {::choice   map->ChoiceState$Builder
   ::fail     map->FailState$Builder
   ::parallel map->ParallelState$Builder
   ::pass     map->PassState$Builder
   ::succeed  map->SucceedState$Builder
   ::task     map->TaskState$Builder
   ::wait     map->WaitState$Builder})

(def bean-class->state-kw
  {ChoiceState   ::choice
   FailState     ::fail
   ParallelState ::parallel
   PassState     ::pass
   SucceedState  ::succeed
   TaskState     ::task
   WaitState     ::wait})

(defmethod bd/->map-val ::states [_ states]
  (into {}
        (map (fn [[name state]]
               [name (assoc (bd/bean->map state)
                       ::state-type
                       (bean-class->state-kw (class state)))]))
        states))

(defmethod bd/builder-override [StateMachine ::states] [_ ^StateMachine$Builder builder states]
  (doseq [[name state] (states->bean-map states)]
    (.state builder name state)))

(def-builder-translation StateMachine
                         #{::comment ::start-at ::states ::timeout-seconds})

(bd/def-translation CreateActivityRequest #{::name})
(bd/def-translation CreateActivityResult #{[:activity-arn ::arn] ::creation-date})

(bd/def-translation CreateStateMachineRequest #{::name [::definition StateMachine] ::role-arn})

(defmethod bd/->bean-val ::definition [_ definition]
  (map->StateMachine definition))

(bd/def-translation CreateStateMachineResult #{[:state-machine-arn ::arn] ::creation-date})
(bd/def-translation DeleteActivityRequest #{[:activity-arn ::arn]})
(bd/def-translation DeleteStateMachineRequest #{[:state-machine-arn ::arn]})
(bd/def-translation DescribeActivityRequest #{[:activity-arn ::arn]})
(bd/def-translation DescribeActivityResult #{[:activity-arn ::arn] ::name ::creation-date})
(bd/def-translation DescribeExecutionRequest #{[:execution-arn ::arn]})

(defmethod bd/->map-val ::input [_ input]
  (ser/deser-io-doc input))

(defmethod bd/->bean-val ::input [_ input]
  (ser/ser-io-doc input))

(defmethod bd/->map-val ::output [_ input]
  (ser/deser-io-doc input))

(defmethod bd/->bean-val ::output [_ output]
  (ser/ser-io-doc output))

(bd/def-translation DescribeExecutionResult #{[:execution-arn ::arn]
                                              ::state-machine-arn
                                              ::name
                                              [::status String]
                                              ::start-date
                                              ::stop-date
                                              ::input
                                              ::output})

(bd/def-translation DescribeStateMachineRequest #{[:state-machine-arn ::arn]})

(defmethod bd/->map-val ::definition [_ definition]
  (-> (StateMachine/fromJson definition)
      (.build)
      (StateMachine->map)))

(bd/def-translation DescribeStateMachineResult #{[:state-machine-arn ::arn]
                                                 ::name
                                                 [::status String]
                                                 ::definition
                                                 ::role-arn
                                                 ::creation-date})

(bd/def-translation GetActivityTaskRequest #{[:activity-arn ::arn]
                                             ::worker-name})

(bd/def-translation GetActivityTaskResult #{::task-token ::input})

(bd/def-translation GetExecutionHistoryRequest #{[:execution-arn ::arn]
                                                 ::max-results
                                                 ::next-token
                                                 ::reverse-order?})

; TODO state names in event details should be made keywords

(bd/def-translation ActivityFailedEventDetails #{::error ::cause})
(bd/def-translation ActivityScheduleFailedEventDetails #{::error ::cause})
(bd/def-translation ActivityScheduledEventDetails #{::resource
                                                    ::input
                                                    [:timeout-in-seconds ::timeout-seconds]
                                                    [:heartbeat-in-seconds ::heartbeat-seconds]})
(bd/def-translation ActivityStartedEventDetails #{::worker-name})
(bd/def-translation ActivitySucceededEventDetails #{::output})
(bd/def-translation ActivityTimedOutEventDetails #{::error ::cause})
(bd/def-translation ExecutionFailedEventDetails #{::error ::cause})
(bd/def-translation ExecutionStartedEventDetails #{::input ::role-arn})
(bd/def-translation ExecutionSucceededEventDetails #{::output})
(bd/def-translation ExecutionAbortedEventDetails #{::error ::cause})
(bd/def-translation ExecutionTimedOutEventDetails #{::error ::cause})
(bd/def-translation StateEnteredEventDetails #{[:name ::state-name] ::input})
(bd/def-translation StateExitedEventDetails #{[:name ::state-name] ::output})

(bd/def-translation HistoryEvent #{::timestamp
                                   [:type ::event-type String]
                                   [:id ::event-id]
                                   ::previous-event-id
                                   ::activity-failed-event-details
                                   ::activity-schedule-failed-event-details
                                   ::activity-scheduled-event-details
                                   ::activity-started-event-details
                                   ::activity-succeeded-event-details
                                   ::activity-timed-out-event-details
                                   ::execution-failed-event-details
                                   ::execution-started-event-details
                                   ::execution-succeeded-event-details
                                   ::execution-aborted-event-details
                                   ::execution-timed-out-event-details
                                   ::state-entered-event-details
                                   ::state-exited-event-details})

(def event-type->details-key
  {"ActivitySucceeded"      ::activity-succeeded-event-details,
   "ExecutionTimedOut"      ::execution-timed-out-event-details,
   "ExecutionAborted"       ::execution-aborted-event-details,
   "ActivityStarted"        ::activity-started-event-details,
   "ActivityScheduled"      ::activity-scheduled-event-details,
   "ChoiceStateEntered"     ::state-entered-event-details
   "FailStateEntered"       ::state-entered-event-details
   "ParallelStateEntered"   ::state-entered-event-details
   "PassStateEntered"       ::state-entered-event-details
   "SucceedStateEntered"    ::state-entered-event-details
   "TaskStateEntered"       ::state-entered-event-details
   "WaitStateEntered"       ::state-entered-event-details
   "ExecutionStarted"       ::execution-started-event-details,
   "ActivityTimedOut"       ::activity-timed-out-event-details,
   "ActivityScheduleFailed" ::activity-schedule-failed-event-details,
   "ChoiceStateExited"      ::state-exited-event-details
   "FailStateExited"        ::state-exited-event-details
   "ParallelStateExited"    ::state-exited-event-details
   "PassStateExited"        ::state-exited-event-details
   "SucceedStateExited"     ::state-exited-event-details
   "TaskStateExited"        ::state-exited-event-details
   "WaitStateExited"        ::state-exited-event-details
   "ExecutionFailed"        ::execution-failed-event-details,
   "ActivityFailed"         ::activity-failed-event-details,
   "ExecutionSucceeded"     ::execution-succeeded-event-details})

(defn camel->name
  "from Emerick, Grande, Carper 2012 p.70"
  [s]
  (->> (strs/split s #"(?<=[a-z])(?=[A-Z])")
       (map strs/lower-case)
       (interpose \-)
       strs/join
       keyword))

(defmethod bd/->map-val ::events [_ events]
  (into []
        (map (fn [event]
               (let [event       (HistoryEvent->map event)
                     details-key (-> event ::event-type event-type->details-key)]
                 (-> (select-keys event [::timestamp ::event-id])
                     (assoc ::event-type (camel->name (::event-type event)))
                     (merge (event details-key))))))
        events))

(bd/def-translation GetExecutionHistoryResult #{::next-token ::events})

(bd/def-translation ActivityListItem #{[::activity-arn ::arn] ::name})
(bd/def-translation ListActivitiesRequest #{::max-results ::next-token})
(bd/def-translation ListActivitiesResult #{::activities ::next-token})

(bd/def-translation ListExecutionsRequest #{::state-machine-arn
                                            [::status-filter String]
                                            ::next-token
                                            ::max-results})
(bd/def-translation ListExecutionsResult #{::executions ::next-token})
(bd/def-translation ExecutionListItem #{[:execution-arn ::arn]
                                        ::state-machine-arn
                                        ::name
                                        [::status String]
                                        ::start-date
                                        ::stop-date})

(bd/def-translation ListStateMachinesRequest #{::max-results ::next-token})
(bd/def-translation ListStateMachinesResult #{::state-machines ::next-token})
(bd/def-translation StateMachineListItem #{[:state-machine-arn ::arn]
                                           ::name
                                           ::creation-date})

(bd/def-translation SendTaskSuccessRequest #{::task-token ::output})
(bd/def-translation SendTaskFailureRequest #{::cause ::error ::task-token})
(bd/def-translation SendTaskHeartbeatRequest #{::task-token})

(bd/def-translation StartExecutionRequest #{::state-machine-arn ::input ::name})
(bd/def-translation StartExecutionResult #{[:execution-arn ::arn] ::start-date})

(bd/def-translation StopExecutionRequest #{[:execution-arn ::arn] ::cause ::error})

