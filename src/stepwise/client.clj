(ns stepwise.client
  (:require [stepwise.model :as mdl]
            [stepwise.sugar :as sgr])
  (:import (com.amazonaws.services.stepfunctions AWSStepFunctionsClient)
           (com.amazonaws.regions Regions Region)))

(set! *warn-on-reflection* true)

(def default-client
  (atom (AWSStepFunctionsClient.)))

(def regions
  (into #{}
        (map (fn [region]
               (.getName ^Regions region)))
        (Regions/values)))

(defn set-default-region! [region]
  (if (contains? regions region)
    (swap! default-client
           (fn [^AWSStepFunctionsClient client]))))

(defn syms->pairs [syms]
  (into []
        (mapcat #(vector (keyword (name 'stepwise.model) (name %))
                         %))
        syms))

(defmacro syms->map [& symbols]
  `(hash-map ~@(syms->pairs symbols)))

(defn create-activity
  ([name]
   (create-activity @default-client name))
  ([^AWSStepFunctionsClient client name]
   (->> (syms->map name)
        mdl/map->CreateActivityRequest
        (.createActivity client)
        mdl/CreateActivityResult->map
        ::mdl/arn)))

(defn create-state-machine
  ([name definition role-arn]
   (create-state-machine @default-client name definition role-arn))
  ([^AWSStepFunctionsClient client name definition role-arn]
   (let [definition (sgr/desugar definition)]
     (->> (syms->map name
                     role-arn
                     definition)
          mdl/map->CreateStateMachineRequest
          (.createStateMachine client)
          mdl/CreateStateMachineResult->map
          ::mdl/arn))))

(defn delete-activity
  ([arn] (delete-activity @default-client arn))
  ([^AWSStepFunctionsClient client arn]
   (.deleteActivity client (mdl/map->DeleteActivityRequest (syms->map arn)))
   nil))

(defn delete-state-machine
  ([arn] (delete-state-machine @default-client arn))
  ([^AWSStepFunctionsClient client arn]
   (.deleteStateMachine client (mdl/map->DeleteActivityRequest (syms->map arn)))
   nil))

(defn describe-activity
  ([arn] (describe-activity @default-client arn))
  ([^AWSStepFunctionsClient client arn]
   (->> (syms->map arn)
        mdl/map->DescribeActivityRequest
        (.describeActivity client)
        mdl/DescribeActivityResult->map)))

(defn describe-execution
  ([arn] (describe-execution @default-client arn))
  ([^AWSStepFunctionsClient client arn]
   (->> (syms->map arn)
        mdl/map->DescribeExecutionRequest
        (.describeExecution client)
        mdl/DescribeExecutionResult->map)))

(defn describe-state-machine
  ([arn] (describe-state-machine @default-client arn))
  ([^AWSStepFunctionsClient client arn]
   (let [description (->> (syms->map arn)
                          mdl/map->DescribeStateMachineRequest
                          (.deleteStateMachine client)
                          mdl/DescribeStateMachineResult->map)]
     (update description ::mdl/definition sgr/sugar))))

(defn get-activity-task
  ([arn]
   (get-activity-task arn nil))
  ([arn {:keys [worker-name] :as opts}]
   (get-activity-task @default-client arn opts))
  ([^AWSStepFunctionsClient client arn {:keys [worker-name]}]
   (->> (syms->map arn worker-name)
        mdl/map->GetActivityTaskRequest
        (.getActivityTask client)
        mdl/GetActivityTaskResult->map)))

(defn get-execution-history
  ([arn]
   (get-execution-history arn nil))
  ([arn {:keys [max-results next-token reverse-order?] :as opts}]
   (get-execution-history @default-client arn opts))
  ([^AWSStepFunctionsClient client arn {:keys [max-results next-token reverse-order?]}]
   (->> (syms->map arn max-results next-token reverse-order?)
        mdl/map->GetExecutionHistoryRequest
        (.getExecutionHistory client)
        mdl/GetExecutionHistoryResult->map)))

(defn list-activities
  ([] (list-activities nil))
  ([{:keys [max-results next-token] :as opts}]
   (list-activities @default-client opts))
  ([^AWSStepFunctionsClient client {:keys [max-results next-token]}]
   (->> (syms->map max-results next-token)
        mdl/map->ListActivitiesRequest
        (.listActivities client)
        mdl/ListActivitiesResult->map)))

(defn list-executions
  ([state-machine-arn]
   (list-executions state-machine-arn nil))
  ([state-machine-arn {:keys [status-filter next-token max-results] :as opts}]
   (list-executions @default-client state-machine-arn opts))
  ([^AWSStepFunctionsClient client state-machine-arn {:keys [status-filter next-token max-results]}]
   (->> (syms->map state-machine-arn status-filter next-token max-results)
        mdl/map->ListExecutionsRequest
        (.listExecutions client)
        mdl/ListExecutionsResult->map)))

(defn list-state-machines
  ([] (list-state-machines nil))
  ([{:keys [max-results next-token] :as opts}]
   (list-state-machines @default-client opts))
  ([^AWSStepFunctionsClient client {:keys [max-results next-token]}]
   (->> (syms->map max-results next-token)
        mdl/map->ListStateMachinesRequest
        (.listStateMachines client)
        mdl/ListStateMachinesResult->map)))

(defn send-task-failure
  ([task-token]
   (send-task-failure task-token nil))
  ([task-token {:keys [cause error] :as opts}]
   (send-task-failure @default-client task-token opts))
  ([^AWSStepFunctionsClient client task-token {:keys [cause error]}]
   (->> (syms->map task-token cause error)
        mdl/map->SendTaskFailureRequest
        (.sendTaskFailure client))
   nil))

(defn send-task-heartbeat
  ([task-token]
   (send-task-heartbeat @default-client task-token))
  ([^AWSStepFunctionsClient client task-token]
   (->> (syms->map task-token)
        mdl/map->SendTaskHeartbeatRequest
        (.sendTaskHeartbeat client))
   nil))

(defn start-execution
  ([state-machine-arn]
   (start-execution state-machine-arn nil))
  ([state-machine-arn {:keys [input name] :as opts}]
   (start-execution @default-client state-machine-arn opts))
  ([^AWSStepFunctionsClient client state-machine-arn {:keys [input name]}]
   (->> (syms->map state-machine-arn input name)
        mdl/map->StartExecutionRequest
        (.startExecution client)
        mdl/StartExecutionResult->map
        ::mdl/arn)))

(defn stop-execution
  ([arn]
   (stop-execution arn nil))
  ([arn {:keys [cause error] :as opts}]
   (stop-execution @default-client arn opts))
  ([^AWSStepFunctionsClient client arn {:keys [cause error]}]
   (->> (syms->map arn cause error)
        mdl/map->StopExecutionRequest
        (.stopExecution client))
   nil))

