(ns stepwise.reloaded
  (:require [stepwise.client :as client]
            [stepwise.arns :as arns]
            [stepwise.model :as mdl]
            [stepwise.core :as core]
            [clojure.string :as strs]
            [stepwise.activities :as activities]
            [clojure.set :as sets]
            [stepwise.sugar :as sgr]
            [stepwise.iam :as iam]
            [stepwise.workers :as workers])
  (:import (com.amazonaws.services.stepfunctions.model StateMachineDeletingException
                                                       StateMachineDoesNotExistException
                                                       ExecutionDoesNotExistException)
           (java.util UUID)))

; Issues
; ------
; 1) Background long polling steals tasks from fresh handler
; 2) Changes to state machine require new one to be created
;
; + find all state machines in namespace
; + find their executions and stop them
; + delete state machines
; + find all activities
; + delete all activities
; + new state machine name: lowest possible
; + new activity name: last one plus one -> cycles/min then reset

(def max-cycles-per-minute 120)
(def version-delimiter "_SNAPSHOT")
(def version-delimiter-re (re-pattern version-delimiter))

(defn get-next-version [current-arns]
  (let [arn      (-> current-arns sort reverse first)
        last-ver (some-> arn
                         (strs/split version-delimiter-re)
                         second
                         (Integer/parseInt))]
    (if last-ver
      (if (>= last-ver max-cycles-per-minute)
        0
        (+ last-ver 1))
      0)))

(def version-format
  (str "%0" (count (str max-cycles-per-minute)) "d"))

(defn deversion-name [nm]
  (first (strs/split (name nm)
                     version-delimiter-re)))

(defn version-name [version nm]
  (keyword (namespace nm)
           (str (name nm)
                version-delimiter
                (format version-format version))))

(defn purge-machines [arns]
  (doseq [arn arns]
    (doseq [execution (try (::mdl/executions (client/list-executions arn))
                           (catch StateMachineDoesNotExistException _))]
      (try (client/stop-execution (::mdl/arn execution))
           (catch ExecutionDoesNotExistException _)))
    (try (client/delete-state-machine arn)
         (catch StateMachineDeletingException _))))

(defn purge-activities [arns]
  (doseq [arn arns]
    (client/delete-activity arn)))

(defn get-family-arns [env-name machine-name]
  (into #{}
        (comp (filter #(= (deversion-name (::mdl/name %))
                          (arns/make-name env-name machine-name)))
              (map ::mdl/arn))
        (::mdl/state-machines (client/list-state-machines))))

(defn run-execution [env-name machine-name definition task-handlers input]
  (let [machine-arns     (get-family-arns env-name machine-name)
        version          (get-next-version machine-arns)
        machine-name     (version-name version machine-name)
        activity-names   (activities/get-names definition)
        member-activity? (into #{}
                               (map (partial arns/make-name env-name))
                               activity-names)
        activity-arns    (into #{}
                               (comp (filter #(member-activity? (deversion-name (::mdl/name %))))
                                     (map ::mdl/arn))
                               (::mdl/activities (client/list-activities)))]

    (purge-machines machine-arns)
    (purge-activities activity-arns)

    (let [activity->arn (into {}
                              (map #(vector % (activities/ensure env-name
                                                                 (version-name version %)))
                                   (keys task-handlers)))
          definition    (->> definition
                             (sgr/desugar)
                             (activities/resolve-names activity->arn))
          _             (client/create-state-machine (arns/make-name env-name machine-name)
                                                     definition
                                                     (iam/ensure-execution-role))
          workers       (workers/boot (-> task-handlers
                                          (sets/rename-keys activity->arn)
                                          (activities/compile-all)))
          result        (core/run-execution env-name machine-name {:input          input
                                                                   :execution-name (str (UUID/randomUUID))})]
      (core/kill-workers workers)
      result)))

; TODO clean-up function to purge all snapshot machines/activities for an env/machine name pair

