(ns stepwise.core
  (:require [stepwise.client :as client]
            [stepwise.iam :as iam]
            [stepwise.s3 :as s3]
            [stepwise.sugar :as sgr]
            [stepwise.activities :as activities]
            [stepwise.workers :as workers]
            [stepwise.model :as mdl]
            [stepwise.arns :as arns]
            [clojure.set :as sets]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk])
  (:import (com.amazonaws.services.stepfunctions.model StateMachineAlreadyExistsException)))

(defn denamespace-keys [response]
  (walk/prewalk (sgr/renamespace-keys (constantly true) nil)
                response))

(defn ensure-state-machine
  "Create state machine `name` if not exist; otherwise update with definition.

   Arguments:
   name       - state machine keyword name
   definition - define a state machine using EDN States Language. See README.md"
  [name definition]
  (let [definition         (sgr/desugar definition)
        activity-name->arn (activities/ensure-all (activities/get-names definition))
        definition         (activities/resolve-kw-resources activity-name->arn definition)
        name-ser           (arns/make-name name)
        arn                (arns/get-state-machine-arn name-ser)
        execution-role     (iam/ensure-execution-role)]
    (try
      (client/create-state-machine name-ser definition execution-role)
      (catch StateMachineAlreadyExistsException _
        (client/update-state-machine arn definition execution-role)))))

(defn describe-execution [arn]
  (-> (client/describe-execution arn)
      (denamespace-keys)))

(defn describe-state-machine [arn]
  (->> (update (client/describe-state-machine arn)
               ::mdl/definition
               sgr/sugar)
       (activities/resolve-resources arns/resource-arn->kw)
       (denamespace-keys)))

(defn start-execution!
  "Starts a state machine execution. Non-blocking variant. For blocking, use (start-execution!!)"
  ([state-machine-name]
   (start-execution! state-machine-name nil))
  ([state-machine-name {:keys [input execution-name]}]
   ; TODO always include these(?)
   (let [input (if execution-name
                 (assoc input :state-machine-name state-machine-name
                              :execution-name execution-name)
                 input)]
     (client/start-execution (arns/get-state-machine-arn state-machine-name)
                             {:input input
                              ; TODO nil execution-name here causes step functions to gen one?
                              :name  execution-name}))))

(defn- await-execution [execution-arn]
  ; TODO occasionally not long enough for execution to even be visible yet -- catch
  (Thread/sleep 500)
  (loop [execution (client/describe-execution execution-arn)]
    (log/debug "Polled execution status"
               (prn-str {:arn    execution-arn
                         :status (::mdl/status execution)}))
    (if (= (::mdl/status execution) "RUNNING")
      ; TODO should do a backoff here to cover both short and longer running executions gracefully
      (do (Thread/sleep 500)
          (recur (client/describe-execution execution-arn)))
      (denamespace-keys execution))))

(defn start-execution!!
  "Starts a state machine execution. Blocking variant. For non-blocking, use (start-execution!)"
  ([state-machine-name]
   (start-execution!! state-machine-name nil))
  ([state-machine-name {:keys [input execution-name] :as opts}]
   (await-execution (start-execution! state-machine-name opts))))

(defn start-workers!
  "Starts activity workers in the background. `task-handlers` is a map of
   the form {<activity-resource-keyword> <handler fn>}. Where,

   <activity-resource-keyword> must match what has been defined in your state
   machine, see README.md for example.
   <handler fn> could either be a fn [1] or a map with [:handler-fn :intercepters]
   keys [2].

   References:
   [1] See https://github.com/Motiva-AI/stepwise#basic-usage
   [2] See https://github.com/Motiva-AI/stepwise#interceptors"
  ([task-handlers]
   (start-workers! task-handlers nil))
  ([task-handlers {:keys [task-concurrency]}]
   (let [activity->arn (into {}
                             (map (fn [activity-name]
                                    [activity-name (arns/get-activity-arn activity-name)]))
                             (keys task-handlers))
         ; TODO was tripping call throttles
         #_(activities/ensure-all (keys task-handlers))]
     (workers/boot (-> task-handlers
                       (sets/rename-keys activity->arn)
                       (activities/compile-all))
                   (sets/rename-keys task-concurrency activity->arn)))))

(defn wait-all-exit!! [{:keys [exit-chans]}]
  (->> (async/merge exit-chans)
       (async/into #{})
       (async/<!!)))

(defn shutdown-workers [workers]
  (async/>!! (:terminate-chan workers) :shutdown)
  (wait-all-exit!! workers))

(defn kill-workers [workers]
  (async/>!! (:terminate-chan workers) :kill)
  (wait-all-exit!! workers))

(def offload-select-keys-to-s3 s3/offload-select-keys-if-large-payload)

