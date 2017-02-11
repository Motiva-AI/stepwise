(ns stepwise.model
  (:require [bean-dip.core :as bd])
  (:import (com.amazonaws.services.stepfunctions.model CreateActivityRequest CreateActivityResult
                                                       CreateStateMachineRequest
                                                       CreateStateMachineResult)
           (com.amazonaws.services.stepfunctions.builder StateMachine StateMachine$Builder)
           (com.amazonaws.services.stepfunctions.builder.states ChoiceState FailState ParallelState
                                                                PassState SucceedState TaskState
                                                                WaitState Choice EndTransition
                                                                NextStateTransition Branch Catcher
                                                                Retrier WaitForSeconds
                                                                WaitForTimestampPath
                                                                WaitForSecondsPath WaitForTimestamp
                                                                Branch$Builder ParallelState$Builder
                                                                Transition$Builder
                                                                TaskState$Builder
                                                                Catcher$Builder Retrier$Builder
                                                                ChoiceState$Builder)
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
                                                                    OrCondition$Builder)
           (java.util Date)))

(set! *warn-on-reflection* true)

(defn builder-form [bean-class-sym]
  (list (symbol (str bean-class-sym) "builder")))

(defmacro def-builder-translation [bean-class-sym field-specs & exclude-fields]
  `(bd/def-builder-translation ~bean-class-sym
                               ~field-specs
                               ~(builder-form bean-class-sym)
                               ~@exclude-fields))

(def-builder-translation WaitForTimestamp #{::timestamp})
(def-builder-translation WaitForSecondsPath #{::seconds-path})
(def-builder-translation WaitForTimestampPath #{::timestamp-path})
(def-builder-translation WaitForSeconds #{::seconds})

(def wait-kw->map->Bean
  {::timestamp      map->WaitForTimestamp
   ::seconds-path   map->WaitForSecondsPath
   ::timestamp-path map->WaitForTimestampPath
   ::seconds        map->WaitForSeconds})

(defmethod bd/->bean-val ::wait-for [_ wait-for]
  ((wait-kw->map->Bean (-> wait-for first key)) wait-for true))

(defmethod bd/builder-override [Retrier ::error-equals] [_ ^Retrier$Builder builder error-equals]
  (doseq [error error-equals]
    (.errorEquals builder error)))

(def-builder-translation Retrier #{::backoff-rate ::error-equals ::interval-seconds ::max-attempts})

(def-builder-translation NextStateTransition #{::next-state-name ::terminal?} ::terminal?)

(def-builder-translation EndTransition #{::terminal?} ::terminal?)

(defn map->Transition ^Transition$Builder [transition]
  (if (= transition ::end)
    (map->EndTransition {} true)
    (map->NextStateTransition {::next-state-name transition} true)))

(defmethod bd/->bean-val ::transition [_ transition]
  (map->Transition transition))

(defmethod bd/builder-override [Catcher ::error-equals] [_ ^Catcher$Builder builder error-equals]
  (doseq [error error-equals]
    (.errorEquals builder error)))

(def-builder-translation Catcher #{::error-equals ::result-path ::transition})

(def-builder-translation Branch #{::comment ::start-at ::states})

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
  {::and         map->AndCondition
   ::or          map->OrCondition
   ::not         map->NotCondition
   ::bool-eq     map->BooleanEqualsCondition
   ::numeric-eq  map->NumericEqualsCondition
   ::numeric-gt  map->NumericGreaterThanCondition
   ::numeric-gte map->NumericGreaterThanOrEqualCondition
   ::numeric-lt  map->NumericLessThanCondition
   ::numeric-lte map->NumericLessThanOrEqualCondition
   ::str-eq      map->StringEqualsCondition
   ::str-gt      map->StringGreaterThanCondition
   ::str-gte     map->StringGreaterThanOrEqualCondition
   ::str-lt      map->StringLessThanCondition
   ::str-lte     map->StringLessThanOrEqualCondition
   ::ts-eq       map->TimestampEqualsCondition
   ::ts-gt       map->TimestampGreaterThanCondition
   ::ts-gte      map->TimestampGreaterThanOrEqualCondition
   ::ts-lt       map->TimestampLessThanCondition
   ::ts-lte      map->TimestampLessThanOrEqualCondition})

(defn tuple->Condition [[condition attrs]]
  ((condition-kw->map->Bean condition) attrs true))

(defmethod bd/->bean-val ::condition [_ conditions]
  (tuple->Condition conditions))

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
    (.choice builder (map->Choice choice true))))

(def-builder-translation ChoiceState
                         #{::choices ::comment ::default-state-name ::input-path ::output-path
                           ::terminal-state?}
                         ::terminal-state?)

(def-builder-translation FailState #{::cause ::comment ::error})

(defmethod bd/builder-override [ParallelState ::branches] [_ ^ParallelState$Builder builder branches]
  (doseq [branch branches]
    (.branch builder (map->Branch branch true))))

(defmethod bd/builder-override [ParallelState ::catchers] [_ ^ParallelState$Builder builder catchers]
  (doseq [catcher catchers]
    (.branch builder (map->Catcher catcher true))))

(defmethod bd/builder-override [ParallelState ::retriers] [_ ^ParallelState$Builder builder retriers]
  (doseq [retrier retriers]
    (.branch builder (map->Retrier retrier true))))

(def-builder-translation ParallelState
                         #{::branches ::catchers ::comment ::input-path ::output-path ::result-path
                           ::retriers ::transition})

(def-builder-translation PassState
                         #{::comment ::input-path ::output-path ::result ::result-path
                           ::transition})

(def-builder-translation SucceedState
                         #{::comment ::input-path ::output-path ::terminal-state?}
                         ::terminal-state?)

(defmethod bd/builder-override [TaskState ::catchers] [_ ^TaskState$Builder builder catchers]
  (doseq [catcher catchers]
    (.catcher builder (map->Catcher catcher true))))

(defmethod bd/builder-override [TaskState ::retriers] [_ ^TaskState$Builder builder retriers]
  (doseq [retrier retriers]
    (.retrier builder (map->Retrier retrier true))))

(def-builder-translation TaskState
                         #{::catchers ::comment ::heartbeat-seconds ::input-path ::output-path
                           ::resource ::result-path ::retriers ::timeout-seconds ::transition})

(def-builder-translation WaitState
                         #{::comment ::input-path ::output-path ::transition ::wait-for})

(def state-kw->map->Bean
  {::choice   map->ChoiceState
   ::fail     map->FailState
   ::parallel map->ParallelState
   ::pass     map->PassState
   ::succeed  map->SucceedState
   ::task     map->TaskState
   ::wait     map->WaitState})

(defn states->bean-map [states]
  (into {}
        (map (fn [[name {:keys [::type] :as attrs}]]
               [name ((state-kw->map->Bean type) attrs true)]))
        states))

(defmethod bd/builder-override [Branch ::states] [_ ^Branch$Builder builder states]
  (doseq [[name state] (states->bean-map states)]
    (.state builder name state)))

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
               [name [(bean-class->state-kw (class state))
                      (bd/bean->map state)]]))
        states))

(defmethod bd/builder-override [StateMachine ::states] [_ ^StateMachine$Builder builder states]
  (doseq [[name state] (states->bean-map states)]
    (.state builder name state)))

(def-builder-translation StateMachine
                         #{::comment ::start-at ::states ::timeout-seconds})

(bd/def-translation CreateActivityRequest #{::name})
(bd/def-translation CreateActivityResult #{::activity-arn ::creation-date})

(bd/def-translation CreateStateMachineRequest #{::name [::definition StateMachine] ::role-arn})

(defmethod bd/->bean-val ::definition [_ definition]
  (map->StateMachine definition))

(bd/def-translation CreateStateMachineResult #{::state-machine-arn ::creation-date})

