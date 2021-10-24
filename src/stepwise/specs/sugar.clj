(ns stepwise.specs.sugar
  (:require [clojure.spec.alpha :as s]
            [stepwise.model :as mdl]
            [stepwise.specs.model]
            [stepwise.sugar :as sgr]
            [clojure.spec.gen.alpha :as gen]
            [clojure.walk :as walk])
  (:import (java.util Date)
           (clojure.lang LazySeq)))

(defn string-alphanumeric []
  (gen/gen-for-name 'clojure.test.check.generators/string-alphanumeric))

(defn gen-keywords []
  (gen/fmap (fn [[ns kw]]
              (keyword ns kw))
            (gen/tuple (gen/one-of [(gen/return nil)
                                    (gen/such-that not-empty
                                                   (string-alphanumeric))])
                       (string-alphanumeric))))

(s/def ::sgr/keyword
  ; TODO constrain
  (s/with-gen (s/and keyword?
                     #(not-empty (name %)))
              gen-keywords))

(s/def ::sgr/state-name ::sgr/keyword)

(s/def ::sgr/next ::sgr/state-name)

(s/def ::sgr/transition
  (s/with-gen (fn [transition]
                (or (keyword? (:next transition))
                    (= (:end transition) true)))
              (constantly (gen/elements [{:next :bleh}
                                         {:end true}]))))

(defmacro constant [value]
  `(s/with-gen #(= ~value %)
               (constantly (gen/return ~value))))

(s/def ::sgr/and
  (s/cat :op (constant :and)
         :conds (s/+ ::sgr/condition)))

(s/def ::sgr/or
  (s/cat :op (constant :or)
         :conds (s/+ ::sgr/condition)))

(s/def ::sgr/not
  (s/cat :op (constant :not)
         :conds ::sgr/condition))

(s/def ::sgr/comparable
  (s/or :int int?
        :double double?
        :string string?
        :date (s/with-gen #(instance? Date %)
                          (constantly (gen/return (Date.))))))

(defmacro defcompare [spec-kw op-kw]
  `(s/def ~spec-kw
     (s/cat :op (constant ~op-kw)
            :variable ::mdl/reference-path
            :value ::sgr/comparable)))

(defcompare ::sgr/eq :=)
(defcompare ::sgr/gt :>)
(defcompare ::sgr/gte :>=)
(defcompare ::sgr/lt :<)
(defcompare ::sgr/lte :<=)

(s/def ::sgr/bool ::mdl/reference-path)

(s/def ::sgr/condition
  (s/or :and ::sgr/and
        :or ::sgr/or
        :not ::sgr/not
        :bool ::sgr/bool
        :eq ::sgr/eq
        :gt ::sgr/gt
        :lt ::sgr/lt
        :lte ::sgr/lte))

(s/def ::sgr/choices
  (s/coll-of (s/merge (s/keys :req-un [::sgr/condition])
                      (s/get-spec ::sgr/transition))
             :gen-max 3))

(s/def ::sgr/default ::sgr/state-name)

(defn type= [type-name]
  (let [unqual-name (-> type-name name keyword)]
    (s/with-gen #(-> % :type (= unqual-name))
                (constantly (gen/return {:type unqual-name})))))

; TODO file bug w/ AWS -- default state name supposed to be optional but their validation fails w/o
(s/def ::sgr/choice-state
  (s/merge (type= :choice)
           (s/keys :req-un [::sgr/choices ::sgr/default]
                   :opt-un [::mdl/comment ::mdl/input-path ::mdl/output-path])))

(s/def ::sgr/branches
  (s/coll-of (s/keys :req-un [::sgr/start-at ::sgr/states]
                     :opt-un [::mdl/comment])
             :gen-max 3))

; also allow free-form strings for handling those from non clojure/keyword tasks
(s/def ::sgr/error ::sgr/keyword)

(s/def ::sgr/error-equals
  (s/or :set (s/coll-of ::sgr/error
                        :gen-max 3
                        :min-count 2)
        :single ::sgr/error))

(s/def ::sgr/catch
  (s/coll-of (s/keys :req-un [::sgr/error-equals ::sgr/next]
                     :opt-un [::mdl/result-path])
             :gen-max 3))

(s/def ::sgr/retry
  (s/coll-of (s/keys :req-un [::sgr/error-equals]
                     :opt-un [::mdl/backoff-rate ::mdl/interval-seconds ::mdl/max-attempts])
             :gen-max 3))

(s/def ::sgr/parallel
  (s/merge (type= ::mdl/parallel)
           (s/keys :req-un [::sgr/branches]
                   :opt-un [::mdl/comment
                            ::mdl/input-path ::mdl/output-path
                            ::mdl/parameters ::mdl/result-path ::mdl/result-selector
                            ::sgr/catch ::sgr/retry])
           (s/get-spec ::sgr/transition)))

(s/def ::sgr/result
  (s/with-gen map?
              (constantly (gen/return {:some "results"}))))

(s/def ::sgr/pass
  (s/merge (type= ::mdl/pass)
           (s/keys :opt-un [::mdl/comment
                            ::mdl/input-path ::mdl/output-path
                            ::mdl/parameters ::mdl/result-path ::sgr/result])
           (s/get-spec ::sgr/transition)))

(s/def ::sgr/task
  (s/merge (type= ::mdl/task)
           (s/keys :req-un [::mdl/resource]
                   :opt-un [::mdl/comment ::mdl/input-path ::mdl/output-path
                            ::sgr/catch ::sgr/retry
                            ::mdl/parameters ::mdl/result-selector ::mdl/result-path
                            ::mdl/timeout-seconds ::mdl/heartbeat-seconds])
           (s/get-spec ::sgr/transition)))

(s/def ::sgr/wait-for
  (s/merge (type= ::mdl/wait-for)
           (s/with-gen (fn [wait-for]
                         (or (instance? Date (:timestamp wait-for))
                             (string? (:timestamp-path wait-for))
                             (try
                               (int (:seconds wait-for))
                               true
                               (catch Throwable _ false))
                             (string? (:seconds-path wait-for))))
                       (constantly (gen/return {:seconds 10})))
           (s/keys :opt-un [::mdl/comment ::mdl/input-path ::mdl/output-path])
           (s/get-spec ::sgr/transition)))

(s/def ::sgr/fail
  (s/merge (type= ::mdl/fail)
           (s/keys :req-un [::sgr/error ::mdl/cause]
                   :opt-un [::mdl/comment])))

(s/def ::sgr/succeed
  (s/merge (type= ::mdl/succeed)
           (s/keys :opt-un [::mdl/comment ::mdl/input-path ::mdl/output-path])))

(s/def ::sgr/state
  (s/or :wait-for ::sgr/wait-for
        :choice ::sgr/choice-state
        :pass ::sgr/pass
        :task ::sgr/task
        :parallel ::sgr/parallel
        :fail ::sgr/fail
        :succeed ::sgr/succeed))

(s/def ::sgr/start-at ::sgr/state-name)

(s/def ::sgr/states
  (s/map-of ::sgr/state-name
            ::sgr/state))

(s/def ::sgr/state-machine
  (s/keys :req-un [::sgr/start-at ::sgr/states]
          :opt-un [::mdl/comment ::mdl/timeout-seconds]))

(defn lazy-seq->vec [node]
  (if (instance? LazySeq node)
    (vec node)
    node))

(defn get-gen []
  (let [condition-orig    (s/gen ::sgr/condition)
        error-equals-orig (s/gen ::sgr/error-equals)]
    (s/gen ::sgr/state-machine
           {::sgr/condition    (constantly (gen/fmap (partial walk/prewalk lazy-seq->vec)
                                                     condition-orig))
            ::sgr/error-equals (constantly (gen/fmap (fn [error-equals]
                                                       (if (seqable? error-equals)
                                                         (set error-equals)
                                                         error-equals))
                                                     error-equals-orig))})))

