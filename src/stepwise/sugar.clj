(ns stepwise.sugar
  (:require [stepwise.model :as mdl]
            [clojure.set :as sets]
            [clojure.string :as strs])
  (:import (java.util Date)))

{:start-at :hello
 :states   {:hello  {:type     :task
                     :resource :foolio
                     :next     :foolio}
            :foolio {:type     :task
                     :resource :bam
                     :end      true}}}

(defn model-keyword [n]
  (keyword (name 'stepwise.model) (name n)))

(def op->str
  {:=  "eq"
   :>  "gt"
   :>= "gte"
   :<  "lt"
   :<= "lte"})

(def value-class->str
  {String "str"
   Date   "ts"
   Double "numeric"
   Long   "numeric"})

(def value-class->expected-value-kw
  {String ::mdl/expected-value-string
   Date   ::mdl/expected-value-timestamp
   Double ::mdl/expected-value-double
   Long   ::mdl/expected-value-long})

(defn desugar-comparison [[op variable value :as condition]]
  (let [op-str          (op->str op)
        value-class-str (value-class->str (class value))]
    (when-not op-str
      (throw (ex-info "Invalid operation in comparison condition"
                      {:condition condition
                       :valid-ops (keys op->str)})))

    (when-not value-class-str
      (throw (ex-info "Invalid value type in comparison condition"
                      {:condition   condition
                       :valid-types (keys value-class->str)})))

    (when-not (string? variable)
      (throw (ex-info "Variable in comparison condition must be a string"
                      {:condition condition})))

    [(model-keyword (str value-class-str "-" op-str))
     {::mdl/variable                                 variable
      (value-class->expected-value-kw (class value)) value}]))

(defn desugar-condition [condition]
  (condp #(%1 %2) (first condition)
    char? [::mdl/bool-eq {::mdl/variable               condition
                          ::mdl/expected-value-boolean true}]
    #{:and :or :not} (into [(model-keyword (first condition))]
                           (map desugar-condition)
                           (rest condition))
    #{:= :> :>= :< :<=} (desugar-comparison condition)))

(def str->value-class
  (sets/map-invert value-class->str))

(def str->op
  (sets/map-invert op->str))

(def model-comparison-ops
  #{::mdl/numeric-eq
    ::mdl/numeric-gt
    ::mdl/numeric-gte
    ::mdl/numeric-lt
    ::mdl/numeric-lte
    ::mdl/str-eq
    ::mdl/str-gt
    ::mdl/str-gte
    ::mdl/str-lt
    ::mdl/str-lte
    ::mdl/ts-eq
    ::mdl/ts-gt
    ::mdl/ts-gte
    ::mdl/ts-lt
    ::mdl/ts-lte})

(defn sugar-comparison [op attrs]
  (let [[value-class-str op-str] (strs/split (name op) #"-")
        expected-value-key (value-class->expected-value-kw (str->value-class value-class-str))]
    [(str->op op-str)
     (::mdl/variable attrs)
     (attrs expected-value-key)]))

(defn sugar-condition [[op & map-or-children]]
  (condp #(%1 %2) op
    #{::mdl/bool-eq} (::mdl/variable (first map-or-children))
    #{::mdl/not ::mdl/and ::mdl/or} (into [(keyword (name op))]
                                          (map sugar-condition)
                                          map-or-children)
    model-comparison-ops (sugar-comparison op (first map-or-children))))

(defn sugar-state-type [state-type]
  (name state-type))

(defn desugar-state-type [state-type]
  (keyword state-type))

(defn sugar-transition [transition-having]
  (condp = (::mdl/transition transition-having)
    ::mdl/end (-> transition-having
                  (dissoc ::mdl/transition)
                  (assoc :end true))
    (-> transition-having
        (dissoc ::mdl/transition)
        (assoc :next (desugar-state-type (::mdl/transition transition-having))))))

(defn desugar-transition [transition-having]
  (condp #(contains? %2 %1) transition-having
    :end (-> transition-having
             (dissoc :end)
             (assoc ::mdl/transition ::mdl/end))
    :next (-> transition-having
              (dissoc :next)
              (assoc ::mdl/transition (sugar-state-type (:next transition-having))))))

(def wait-interval-keys
  #{:seconds
    :seconds-path
    :timestamp
    :timestamp-path})

(defmulti desugar-state* :type)
(defmulti sugar-state* ::mdl/type)

(defn desugar-state [state]
  (-> state
      (desugar-state*)
      (dissoc :type)
      (assoc ::mdl/type (model-keyword (:type state)))))

(defn sugar-state [state]
  (-> state
      (sugar-state*)
      (dissoc ::mdl/type)
      (assoc :type (keyword (name (::mdl/type state))))))

(defmethod desugar-state* :wait [wait]
  (let [interval-key (some wait-interval-keys (keys wait))
        wait-for-key (model-keyword interval-key)]
    (-> wait
        (dissoc interval-key)
        (assoc-in [::mdl/wait-for wait-for-key] (wait interval-key))
        (desugar-transition))))

(defmethod sugar-state* ::mdl/wait [wait]
  (let [wait-for-key (-> wait ::mdl/wait-for keys first)
        interval-key (keyword (name wait-for-key))]
    (-> wait
        (dissoc ::mdl/wait-for)
        (assoc interval-key (get-in wait [::mdl/wait-for wait-for-key]))
        (sugar-transition))))

(defn desugar-choice [choice]
  (-> choice
      (dissoc :condition)
      (assoc ::mdl/condition (desugar-condition (:condition choice)))
      (desugar-transition)))

(defn sugar-choice [choice]
  (-> choice
      (dissoc ::mdl/condition)
      (assoc :condition (sugar-condition (::mdl/condition choice)))
      (sugar-transition)))

(defmethod desugar-state* :choice [choice-state]
  (-> choice-state
      (dissoc :choices)
      (assoc ::mdl/choices (mapv desugar-choice (:choices choice-state)))))

(defmethod sugar-state* ::mdl/choice [choice-state]
  (-> choice-state
      (dissoc ::mdl/choices)
      (assoc :choices (mapv sugar-choice (::mdl/choices choice-state)))))

(defmethod desugar-state* :pass [pass]
  (desugar-transition pass))

(defmethod sugar-state* ::mdl/pass [pass]
  (sugar-transition pass))

