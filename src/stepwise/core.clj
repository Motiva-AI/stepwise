(ns stepwise.core
  (:require [stepwise.client :as client]
            [stepwise.iam :as iam]
            [stepwise.sugar :as sgr]
            [stepwise.activities :as activities]
            [stepwise.workers :as workers]
            [stepwise.model :as mdl]
            [stepwise.arns :as arns]
            [clojure.set :as sets]
            [clojure.core.async :as async]))

(defn create-state-machine [env-name name definition]
  (let [definition         (sgr/desugar definition)
        activity-name->arn (activities/ensure-all env-name (activities/get-names definition))
        definition         (activities/resolve-names activity-name->arn definition)]
    (client/create-state-machine (arns/make-name env-name name)
                                 definition
                                 (iam/ensure-execution-role))))

(defn start-execution [ns state-machine-name & [{:keys [input name] :as opts}]]
  (client/start-execution (arns/get-state-machine-arn ns state-machine-name)
                          opts))

(defn run-execution [env-name state-machine-name & [{:keys [input name] :as opts}]]
  (let [execution-arn (client/start-execution (arns/get-state-machine-arn env-name
                                                                          state-machine-name)
                                              opts)]
    ; TODO occasionally not long enough for execution to even be visible yet
    (Thread/sleep 200)
    (loop [execution (client/describe-execution execution-arn)]
      (if (= (::mdl/status execution) "RUNNING")
        ; TODO should do a curve here to cover both short and longer running executions gracefully
        (do (Thread/sleep 500)
            (recur (client/describe-execution execution-arn)))
        execution))
    (client/get-execution-history execution-arn)))

(defn start-workers [env-name task-handlers]
  (let [activity-kw->arn (activities/ensure-all env-name (keys task-handlers))]
    (workers/boot (sets/rename-keys task-handlers activity-kw->arn))))

(defn shutdown-workers [workers]
  (async/>!! (:terminate-chan workers) :shutdown)
  (async/<!! (:exited-chan workers)))

(defn kill-workers [workers]
  (async/>!! (:terminate-chan workers) :kill)
  (async/<!! (:exited-chan workers)))

