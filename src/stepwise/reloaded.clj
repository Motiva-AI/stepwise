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
  (when (re-find version-delimiter-re nm)
    (first (strs/split (name nm)
                       version-delimiter-re))))

(defn version-name [nm version]
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

(defn get-family-arns [machine-name]
  (into #{}
        (comp (filter #(= (deversion-name (::mdl/name %))
                          (arns/make-name machine-name)))
              (map ::mdl/arn))
        (::mdl/state-machines (client/list-state-machines))))

(defn run-execution [machine-name definition task-handlers input]
  (let [machine-arns     (get-family-arns machine-name)
        version          (get-next-version machine-arns)
        machine-name     (version-name machine-name version)
        activity-names   (activities/get-names definition)
        member-activity? (into #{} (map arns/make-name) activity-names)
        ; TODO paginate
        activity-arns    (into #{}
                               (comp (filter #(member-activity? (deversion-name (::mdl/name %))))
                                     (map ::mdl/arn))
                               (::mdl/activities (client/list-activities)))]

    (purge-machines machine-arns)
    (purge-activities activity-arns)

    (let [activity->snapshot (into {}
                                   (map #(vector % (version-name % version)))
                                   activity-names)
          _                  (core/create-state-machine machine-name
                                                        (activities/resolve-kw-resources activity->snapshot
                                                                                         definition))
          workers            (core/start-workers (sets/rename-keys task-handlers
                                                                   activity->snapshot))
          result             (core/run-execution machine-name
                                                 {:input          input
                                                  :execution-name (str (UUID/randomUUID))})]
      (core/kill-workers workers)
      result)))

; TODO clean-up function to purge all snapshot machines/activities for a machine

