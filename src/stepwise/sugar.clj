(ns stepwise.sugar
  (:require [stepwise.model :as mdl]
            [clojure.set :as sets]
            [clojure.string :as strs]
            [clojure.walk :as walk]
            [stepwise.serialization :as json]
            [stepwise.serialization :as ser])
  (:import (java.util Date)
           (clojure.lang MapEntry)))

(defmulti sugar* (fn [k _] k))
(defmulti desugar* (fn [k _] k))

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

(defmethod desugar* ::condition [_ condition]
  (condp #(%1 %2) (first condition)
    char? [::mdl/bool-eq {::mdl/variable               condition
                          ::mdl/expected-value-boolean true}]
    #{:and :or :not} (into [(model-keyword (first condition))]
                           (map (partial desugar* ::condition))
                           (rest condition))
    #{:= :> :>= :< :<=} (desugar-comparison condition)))

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
  (let [[_ op-str] (strs/split (name op) #"-")
        expected-value (-> attrs (dissoc ::mdl/variable) first val)]
    [(str->op op-str)
     (::mdl/variable attrs)
     expected-value]))

(defmethod sugar* ::mdl/condition [_ [op & map-or-children]]
  (condp #(%1 %2) op
    #{::mdl/bool-eq} (::mdl/variable (first map-or-children))
    #{::mdl/not ::mdl/and ::mdl/or} (into [(keyword (name op))]
                                          (map (partial sugar* ::mdl/condition))
                                          map-or-children)
    model-comparison-ops (sugar-comparison op (first map-or-children))))

(defmethod desugar* ::next [_ next-state]
  {::key ::mdl/transition
   ::val (ser/ser-keyword-name next-state)})

(defmethod desugar* ::end [_ _]
  {::key ::mdl/transition
   ::val ::mdl/end})

(defmethod sugar* ::mdl/transition [_ transition]
  (if (= transition ::mdl/end)
    {::key :end
     ::val true}
    {::key :next
     ::val (ser/deser-keyword-name transition)}))

(defmethod desugar* ::type [_ state-type]
  {::key ::mdl/state-type
   ::val (model-keyword state-type)})

(defmethod sugar* ::mdl/state-type [_ state-type]
  {::key :type
   ::val (keyword (name state-type))})

(defmethod desugar* ::seconds [_ seconds]
  {::key ::mdl/wait-for
   ::val {::mdl/seconds seconds}})

(defmethod desugar* ::seconds-path [_ seconds-path]
  {::key ::mdl/wait-for
   ::val {::mdl/seconds-path seconds-path}})

(defmethod desugar* ::timestamp [_ timestamp]
  {::key ::mdl/wait-for
   ::val {::mdl/timestamp timestamp}})

(defmethod desugar* ::timestamp-path [_ timestamp-path]
  {::key ::mdl/wait-for
   ::val {::mdl/timestamp-path timestamp-path}})

(defmethod sugar* ::mdl/wait-for [_ wait-for]
  (let [[k v] (first wait-for)]
    {::key (keyword (name k))
     ::val v}))

(defmethod desugar* ::default [_ state-name]
  {::key ::mdl/default-state-name
   ::val (ser/ser-keyword-name state-name)})

(defmethod sugar* ::mdl/default-state-name [_ state-name]
  {::key :default
   ::val (ser/deser-keyword-name state-name)})

(defmethod desugar* ::error-equals [_ error-equals]
  (into #{}
        (map ser/ser-keyword-name)
        (if (seqable? error-equals)
          error-equals
          #{error-equals})))

(defmethod sugar* ::mdl/error-equals [_ error-equals]
  (if (= (count error-equals) 1)
    (ser/deser-keyword-name (first error-equals))
    (into #{} (map ser/deser-keyword-name) error-equals)))

(defmethod desugar* ::error [_ error]
  (ser/ser-keyword-name error))

(defmethod sugar* ::mdl/error [_ error]
  (ser/deser-keyword-name error))

(defmethod desugar* ::start-at [_ start-at]
  (ser/ser-keyword-name start-at))

(defmethod sugar* ::mdl/start-at [_ start-at]
  (ser/deser-keyword-name start-at))

(defmethod desugar* ::result [_ result]
  (json/ser-io-doc result))

(defmethod sugar* ::mdl/result [_ result]
  (json/deser-io-doc result))

(defmethod desugar* ::retry [_ retriers]
  {::key ::mdl/retriers
   ::val retriers})

(defmethod sugar* ::mdl/retriers [_ retriers]
  {::key :retry
   ::val retriers})

(defmethod desugar* ::catch [_ catchers]
  {::key ::mdl/catchers
   ::val catchers})

(defmethod sugar* ::mdl/catchers [_ catchers]
  {::key :catch
   ::val catchers})

(defn renamespace-keys [match-key? target-ns]
  (fn [node]
    (if (and (instance? MapEntry node)
             (match-key? (key node)))
      (MapEntry. (keyword target-ns (name (key node)))
                 (val node))
      node)))

(defn translate-keys [multifn target-ns]
  (fn [node]
    (if (and (instance? MapEntry node)
             (get-method multifn (key node)))
      (let [result (multifn (key node) (val node))]
        (if (and (map? result)
                 (contains? result ::key))
          (MapEntry. (::key result)
                     (::val result))
          (MapEntry. (keyword target-ns (name (key node)))
                     result)))
      node)))

(defn transform-state-name-keys [transform-fn]
  (fn [node]
    (if (and (map? node)
             (contains? node :states)
             (map? (:states node)))
      (update node
              :states
              (fn [states]
                (into {}
                      (map (fn [[state-name state]]
                             [(transform-fn state-name) state]))
                      states)))
      node)))

(def bare-sugar-keys
  (into #{}
        (map (comp keyword name key))
        (methods desugar*)))

(def pass-through-model-keys
  #{::mdl/comment
    ::mdl/input-path
    ::mdl/output-path
    ::mdl/result-path
    ::mdl/backoff-rate
    ::mdl/interval-seconds
    ::mdl/max-attempts
    ::mdl/heartbeat-seconds
    ::mdl/timeout-seconds
    ::mdl/branches
    ::mdl/choices
    ::mdl/states
    ::mdl/resource
    ::mdl/cause})

(def bare-pass-through-keys
  (into #{}
        (map (comp keyword name))
        pass-through-model-keys))

; from: https://stackoverflow.com/questions/40557159/clojure-pre-post-walk-with-path-from-root
(defn pathwalk [f path e]
  (let [e' (f path e)]
    (cond
      (map? e') (->> e'
                     (map (fn [[k x]] [k (pathwalk f (conj path k) x)]))
                     (into (empty e')))
      (coll? e') (->> e'
                      (map-indexed (fn [i x] (pathwalk f (conj path i) x)))
                      (into (empty e')))
      :else e')))

(defn get-non-model-keys [state-machine]
  (into #{}
        (comp (map first)
              (remove empty?)
              (remove (fn [path]
                        (= (-> path pop last)
                           ::mdl/states)))
              (remove (fn [path]
                        (let [last-node (last path)]
                          (or (int? last-node)
                              (= (namespace last-node) "stepwise.model"))))))
        (tree-seq (fn [[_ value]]
                    (coll? value))
                  (fn [[path value]]
                    (into []
                          (map (fn [[k v]]
                                 [(conj path k) v]))
                          (if (map? value)
                            value
                            (map-indexed vector value))))
                  [[] state-machine])))

(defn desugar [state-machine]
  (let [desugared   (walk/prewalk (comp (translate-keys desugar* (name 'stepwise.model))
                                        (renamespace-keys bare-pass-through-keys (name 'stepwise.model))
                                        (renamespace-keys bare-sugar-keys (name 'stepwise.sugar))
                                        ; desugar state name keys first to avoid collisions
                                        (transform-state-name-keys ser/ser-keyword-name))
                                  state-machine)
        errant-keys (get-non-model-keys desugared)]
    (if (not-empty errant-keys)
      (throw (ex-info (str "Unrecognized keys in state machine definition: " errant-keys)
                      {:key-paths errant-keys})))
    desugared))

(defn sugar [state-machine]
  (walk/prewalk
    ; sugar state name keys last to avoid key collisions
    ; TODO not sure why this doesn't work when instead included in the function composition below
    (transform-state-name-keys ser/deser-keyword-name)
    (walk/prewalk (comp (translate-keys sugar* nil)
                        (renamespace-keys pass-through-model-keys nil))
                  state-machine)))

