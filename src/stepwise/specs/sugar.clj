(ns stepwise.specs.sugar
  (:require [clojure.spec :as s]
            [stepwise.model :as mdl]
            [stepwise.specs.model :as mdls]
            [stepwise.sugar :as sgr]
            [clojure.spec.gen :as gen])
  (:import (java.util Date)))

(s/def ::sgr/next keyword)

(s/def ::sgr/transition
  (s/with-gen (fn [transition]
                (or (keyword? (:next transition))
                    (= (:end transition) true)))
              (constantly (gen/return {:next :bleh
                                       :end  true}))))

(s/def ::sgr/and
  (s/cat :op #(= :and %)
         :conds (s/+ ::sgr/condition)))

(s/def ::sgr/or
  (s/cat :op #(= :or %)
         :conds (s/+ ::sgr/condition)))

(s/def ::sgr/not
  (s/cat :op #(= :not %)
         :conds ::sgr/condition))

(s/def ::sgr/comparable
  (s/or :int int?
        :double double?
        :string string?
        :date #(instance? Date %)))

(defmacro defcompare [spec-kw op-kw]
  `(s/def ~spec-kw
     (s/cat :op #(= % ~op-kw)
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
                      (s/get-spec ::sgr/transition))))

; TODO file bug w/ AWS -- default state name supposed to be optional but their validation fails w/o
(s/def ::sgr/choice-state
  (s/merge (mdls/type= :choice)
           (s/keys :req-un [::sgr/choices ::sgr/default-state-name]
                   :opt-un [::mdl/comment  ::mdl/input-path ::mdl/output-path])))

(s/def ::sgr/branches
  (s/coll-of (s/keys :req-un [::sgr/start-at ::sgr/states]
                     :opt-un [::mdl/comment])))

(s/def ::sgr/error-equals
  (s/or :set (s/coll-of keyword?)
        :single keyword?))

(s/def ::sgr/catchers
  (s/coll-of (s/keys :req-un [::sgr/error-equals ::sgr/next]
                     :opt-un [::mdl/result-path])))

(s/def ::sgr/retriers
  (s/coll-of (s/keys :req-un [::sgr/error-equals]
                     :opt-un [::mdl/backoff-rate ::mdl/interval-seconds ::mdl/max-attempts])))

(s/def ::sgr/parallel
  (s/merge (mdls/type= ::mdl/parallel)
           (s/keys :req-un [::sgr/branches]
                   :opt-un [::sgr/catchers ::sgr/retriers ::mdl/comment ::mdl/input-path
                            ::mdl/output-path ::mdl/result-path])
           (s/get-spec ::sgr/transition)))

(s/def ::sgr/pass
  (s/merge (mdls/type= ::mdl/pass)
           (s/get-spec ::sgr/transition)))

(s/def ::sgr/task
  (s/merge (mdls/type= ::mdl/task)
           (s/keys :req-un [::sgr/resource]
                   :opt-un [::sgr/catchers ::sgr/retriers ::mdl/comment ::mdl/heartbeat-seconds
                            ::mdl/input-path ::mdl/output-path ::mdl/result-path
                            ::mdl/timeout-seconds])
           (s/get-spec ::sgr/transition)))

(s/def ::sgr/wait-for
  (s/merge (mdls/type= ::mdl/wait-for)
           (s/with-gen (fn [wait-for]
                         (or (instance? Date (:timestamp wait-for))
                             (string? (:timestamp-path wait-for))
                             (try
                               (int (:seconds wait-for))
                               true
                               (catch IllegalArgumentException _
                                 false))
                             (string? (:seconds-path wait-for))))
                       (constantly (gen/return {:seconds 10})))
           (s/keys :opt-un [::mdl/comment ::mdl/input-path ::mdl/output-path])
           (s/get-spec ::sgr/transition)))

(s/def ::sgr/state
  (s/or :wait-for ::sgr/wait-for
        :choice ::sgr/choice-state
        :pass ::sgr/pass
        :task ::sgr/task
        :parallel ::sgr/parallel
        :fail ::mdl/fail
        :succeed ::mdl/succeed))

(s/def ::sgr/states
  (s/map-of keyword ::sgr/state))

(s/def ::sgr/state-machine
  (s/keys :req-un [::sgr/start-at ::sgr/states]
          :opt-un [::mdl/comment ::mdl/timeout-seconds]))

