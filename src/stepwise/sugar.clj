(ns stepwise.sugar
  (:require [stepwise.model :as mdl]
            [clojure.set :as sets]
            [clojure.string :as strs]
            [clojure.walk :as walk])
  (:import (java.util Date)
           (clojure.lang MapEntry)))

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

(defn desugar-state-name [state-name]
  (name state-name))

(defn sugar-state-name [state-name]
  (keyword state-name))

(defn translate [amap from-key to-key afn]
  (-> amap
      (dissoc from-key)
      (assoc to-key (afn (amap from-key)))))

(defn sugar-transition [transition-having]
  (condp = (::mdl/transition transition-having)
    ::mdl/end (translate transition-having ::mdl/transition :end (constantly true))
    (translate transition-having ::mdl/transition :next sugar-state-name)))

(defn desugar-transition [transition-having]
  (condp #(contains? %2 %1) transition-having
    :end (translate transition-having
                    :end
                    ::mdl/transition
                    (constantly ::mdl/end))
    :next (translate transition-having
                     :next
                     ::mdl/transition
                     desugar-state-name)))

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
      (translate :type ::mdl/type model-keyword)))

(defn desugar-states [states]
  (into {}
        (map (fn [[state-name state]]
               [(desugar-state-name state-name)
                (desugar-state state)]))
        states))

(defn sugar-state [state]
  (-> state
      (sugar-state*)
      (translate ::mdl/type :type (comp keyword name))))

(defn sugar-states [states]
  (into {}
        (map (fn [[state-name state]]
               [(sugar-state-name state-name)
                (sugar-state state)]))
        states))

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
      (translate :condition ::mdl/condition desugar-condition)
      (desugar-transition)))

(defn sugar-choice [choice]
  (-> choice
      (translate ::mdl/condition :condition sugar-condition)
      (sugar-transition)))

(defmethod desugar-state* :choice [choice-state]
  (-> choice-state
      (translate :choices ::mdl/choices (partial mapv desugar-choice))
      ((fn [choice-state]
         (if-let [state-name (:default-state-name choice-state)]
           (-> choice-state
               (dissoc :default-state-name)
               (assoc ::mdl/default-state-name
                      (desugar-state-name state-name)))
           choice-state)))))

(defmethod sugar-state* ::mdl/choice [choice-state]
  (translate choice-state
             ::mdl/choices
             :choices
             (partial mapv sugar-choice)))

(defmethod desugar-state* :pass [pass]
  (desugar-transition pass))

(defmethod sugar-state* ::mdl/pass [pass]
  (sugar-transition pass))

(defn desugar-error-equals [error-equals-having]
  (translate error-equals-having
             :error-equals
             ::mdl/error-equals
             #(into #{}
                    (map name)
                    (if (seqable? %) % #{%}))))

(defn sugar-error-equals [error-equals-having]
  (translate error-equals-having
             ::mdl/error-equals
             :error-equals
             (fn [error-equals]
               (if (= (count error-equals) 1)
                 (keyword (first error-equals))
                 (into #{} (map keyword) error-equals)))))

(defn desugar-catcher [catcher]
  (-> catcher
      (desugar-error-equals)
      (desugar-transition)))

(defn sugar-catcher [catcher]
  (-> catcher
      (sugar-error-equals)
      (sugar-transition)))

(defmethod desugar-state* :task [task]
  (-> task
      (translate :catchers ::mdl/catchers (partial mapv desugar-catcher))
      (translate :retriers ::mdl/retriers (partial mapv desugar-error-equals))
      (desugar-transition)))

(defmethod sugar-state* ::mdl/task [task]
  (-> task
      (translate ::mdl/catchers :catchers (partial mapv sugar-catcher))
      (translate ::mdl/retriers :retriers (partial mapv sugar-error-equals))
      (sugar-transition)))

(defn desugar-branch [branch]
  (-> branch
      (translate :start-at ::mdl/start-at desugar-state-name)
      (translate :states ::mdl/states desugar-states)))

(defn sugar-branch [branch]
  (-> branch
      (translate ::mdl/start-at :start-at sugar-state-name)
      (translate ::mdl/states :states sugar-states)))

(defmethod desugar-state* :parallel [parallel]
  (-> parallel
      (translate :catchers ::mdl/catchers (partial mapv desugar-catcher))
      (translate :retriers ::mdl/retriers (partial mapv desugar-error-equals))
      (translate :branches ::mdl/branches (partial mapv desugar-branch))
      (desugar-transition)))

(defmethod sugar-state* ::mdl/parallel [parallel]
  (-> parallel
      (translate ::mdl/catchers :catchers (partial mapv sugar-catcher))
      (translate ::mdl/retriers :retriers (partial mapv sugar-error-equals))
      (translate ::mdl/branches :branches (partial mapv sugar-branch))
      (sugar-transition)))

(defn desugar-state-machine [state-machine]
  (let [desugared (-> state-machine
                      (translate :start-at ::mdl/start-at desugar-state-name)
                      (translate :states ::mdl/states desugar-states))]
    (walk/prewalk (fn [node]
                    (if (and (instance? MapEntry node)
                             (keyword? (key node))
                             (nil? (namespace (key node))))
                      [(keyword (name 'stepwise.model) (name (key node)))
                       (val node)]
                      node))
                  desugared)))

(defn sugar-state-machine [state-machine]
  (let [sugared (-> state-machine
                    (translate ::mdl/start-at :start-at desugar-state-name)
                    (translate ::mdl/states :states desugar-states))]
    (walk/prewalk (fn [node]
                    (if (and (instance? MapEntry node)
                             (namespace (key node)))
                      [(keyword (name (key node)))
                       (val node)]
                      node))
                  sugared)))

