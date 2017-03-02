(ns stepwise.core
  (:require [stepwise.client :as client]
            [stepwise.iam :as iam]
            [clojure.walk :as walk]
            [stepwise.sugar :as sgr]
            [stepwise.activities :as activities]
            [stepwise.model :as mdl]
            [stepwise.arns :as arns]
            [clojure.set :as sets]
            [clojure.core.async :as async])
  (:import (clojure.lang MapEntry)))

(defn activity-kw-entry? [node]
  (and (instance? MapEntry node)
       (= (key node) ::mdl/resource)
       (keyword? (val node))))

(defn get-activity-kws [definition]
  (into #{}
        (comp (filter activity-kw-entry?)
              (map val))
        (tree-seq #(or (map? %) (vector? %))
                  identity
                  definition)))

(defn resolve-activity-kws [activity-kw->arn definition]
  (walk/prewalk (fn [node]
                  (if (activity-kw-entry? node)
                    (MapEntry. ::mdl/resource (activity-kw->arn (val node)))
                    node))
                definition))

(defn ensure-activities [ns kw-names]
  (into {}
        (map (fn [kw-name]
               [kw-name (->> kw-name
                             (arns/make-name ns)
                             client/create-activity)]))
        kw-names))

(defn create-state-machine [ns name definition]
  (let [definition       (sgr/desugar definition)
        activity-kw->arn (ensure-activities ns (get-activity-kws definition))
        definition       (resolve-activity-kws activity-kw->arn definition)]
    (client/create-state-machine (arns/make-name ns name)
                                 definition
                                 (iam/ensure-execution-role))))

(defn start-execution [ns state-machine-name & [{:keys [input name] :as opts}]]
  (client/start-execution (arns/get-state-machine-arn ns state-machine-name)
                          opts))

(defn run-execution [ns state-machine-name & [{:keys [input name] :as opts}]]
  (let [execution-arn (client/start-execution (arns/get-state-machine-arn ns state-machine-name)
                                              opts)]
    (loop [execution (client/describe-execution execution-arn)]
      (if (= (::mdl/status execution) "RUNNING")
        (do (Thread/sleep 1000)
            (recur (client/describe-execution execution-arn)))
        execution))
    (client/get-execution-history execution-arn)))

(defn start-workers [ns task-handlers]
  (let [activity-kw->arn (ensure-activities ns (keys task-handlers))]
    (activities/boot (sets/rename-keys task-handlers activity-kw->arn))))

(defn shutdown-workers [workers]
  (async/>!! (:terminate-chan workers) :shutdown)
  (async/<!! (:exited-chan workers)))

(defn kill-workers [workers]
  (async/>!! (:terminate-chan workers) :kill)
  (async/<!! (:exited-chan workers)))

