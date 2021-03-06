(ns stepwise.client
  (:require [stepwise.model :as mdl]
            [clojure.core.async :as async])
  (:import (com.amazonaws.services.stepfunctions AWSStepFunctionsClient
                                                 AWSStepFunctionsClientBuilder)
           (com.amazonaws ClientConfiguration)
           (com.amazonaws.services.stepfunctions.builder StateMachine)))

(set! *warn-on-reflection* true)

(def default-client
  (atom nil))

(def client-config
  (doto (ClientConfiguration.)
    (.setSocketTimeout 70000)
    (.setMaxConnections 75)))

(def stock-default-client
  (delay (-> (AWSStepFunctionsClientBuilder/standard)
             (.withClientConfiguration client-config)
             (.build))))

(defn set-default-client! [^AWSStepFunctionsClient client]
  (reset! default-client client))

(defn get-default-client []
  (if-let [client @default-client]
    client
    @stock-default-client))

(defn get-client-max-connections
  ([^AWSStepFunctionsClient client]
   (-> client
       (.getClientConfiguration)
       (.getMaxConnections)))
  ([]
   (get-client-max-connections (get-default-client))))

(defn syms->pairs [syms]
  (into []
        (mapcat #(vector (keyword (name 'stepwise.model) (name %))
                         %))
        syms))

(defmacro syms->map [& symbols]
  `(hash-map ~@(syms->pairs symbols)))

(defn create-activity
  ([name]
   (create-activity (get-default-client) name))
  ([^AWSStepFunctionsClient client name]
   (->> (syms->map name)
        mdl/map->CreateActivityRequest
        (.createActivity client)
        mdl/CreateActivityResult->map
        ::mdl/arn)))

(defn create-state-machine
  ([name definition role-arn]
   (create-state-machine (get-default-client) name definition role-arn))
  ([^AWSStepFunctionsClient client name definition role-arn]
   (->> (syms->map name
                   role-arn
                   definition)
        mdl/map->CreateStateMachineRequest
        (.createStateMachine client)
        mdl/CreateStateMachineResult->map
        ::mdl/arn)))

(defn update-state-machine
  ([arn definition role-arn]
   (update-state-machine (get-default-client) arn definition role-arn))
  ([^AWSStepFunctionsClient client arn definition role-arn]
   (->> #::mdl {:arn             arn
                :definition-json (.toPrettyJson ^StateMachine
                                                (mdl/map->StateMachine definition))
                :role-arn        role-arn}
        mdl/map->UpdateStateMachineRequest
        (.updateStateMachine client)
        mdl/UpdateStateMachineResult->map)

   arn))

(defn delete-activity
  ([arn] (delete-activity (get-default-client) arn))
  ([^AWSStepFunctionsClient client arn]
   (.deleteActivity client (mdl/map->DeleteActivityRequest (syms->map arn)))
   nil))

(defn delete-state-machine
  ([arn] (delete-state-machine (get-default-client) arn))
  ([^AWSStepFunctionsClient client arn]
   (.deleteStateMachine client (mdl/map->DeleteStateMachineRequest (syms->map arn)))
   nil))

(defn describe-activity
  ([arn] (describe-activity (get-default-client) arn))
  ([^AWSStepFunctionsClient client arn]
   (->> (syms->map arn)
        mdl/map->DescribeActivityRequest
        (.describeActivity client)
        mdl/DescribeActivityResult->map)))

(defn describe-execution
  ([arn] (describe-execution (get-default-client) arn))
  ([^AWSStepFunctionsClient client arn]
   (->> (syms->map arn)
        mdl/map->DescribeExecutionRequest
        (.describeExecution client)
        mdl/DescribeExecutionResult->map)))

(defn describe-state-machine
  ([arn] (describe-state-machine (get-default-client) arn))
  ([^AWSStepFunctionsClient client arn]
   (->> (syms->map arn)
        mdl/map->DescribeStateMachineRequest
        (.describeStateMachine client)
        mdl/DescribeStateMachineResult->map)))

(defn get-activity-task
  ([arn]
   (get-activity-task arn nil))
  ([arn {:keys [worker-name] :as opts}]
   (get-activity-task (get-default-client) arn opts))
  ([^AWSStepFunctionsClient client arn {:keys [worker-name]}]
   (->> (syms->map arn worker-name)
        mdl/map->GetActivityTaskRequest
        (.getActivityTask client)
        mdl/GetActivityTaskResult->map)))

(defn get-execution-history
  ([arn]
   (get-execution-history arn nil))
  ([arn {:keys [max-results next-token reverse-order?] :as opts}]
   (get-execution-history (get-default-client) arn opts))
  ([^AWSStepFunctionsClient client arn {:keys [max-results next-token reverse-order?]}]
   (->> (syms->map arn max-results next-token reverse-order?)
        mdl/map->GetExecutionHistoryRequest
        (.getExecutionHistory client)
        mdl/GetExecutionHistoryResult->map)))

(defn list-activities
  ([] (list-activities nil))
  ([{:keys [max-results next-token] :as opts}]
   (list-activities (get-default-client) opts))
  ([^AWSStepFunctionsClient client {:keys [max-results next-token]}]
   (->> (syms->map max-results next-token)
        mdl/map->ListActivitiesRequest
        (.listActivities client)
        mdl/ListActivitiesResult->map)))

(defn list-executions
  ([state-machine-arn]
   (list-executions state-machine-arn nil))
  ([state-machine-arn {:keys [status-filter next-token max-results] :as opts}]
   (list-executions (get-default-client) state-machine-arn opts))
  ([^AWSStepFunctionsClient client state-machine-arn {:keys [status-filter next-token max-results]}]
   (let [request-map (syms->map state-machine-arn status-filter next-token max-results)]
     (->> (if (nil? (::mdl/status-filter request-map))
            (dissoc request-map ::mdl/status-filter)
            request-map)
          mdl/map->ListExecutionsRequest
          (.listExecutions client)
          mdl/ListExecutionsResult->map))))

(defn list-state-machines
  ([] (list-state-machines nil))
  ([{:keys [max-results next-token] :as opts}]
   (list-state-machines (get-default-client) opts))
  ([^AWSStepFunctionsClient client {:keys [max-results next-token]}]
   (->> (syms->map max-results next-token)
        mdl/map->ListStateMachinesRequest
        (.listStateMachines client)
        mdl/ListStateMachinesResult->map)))

(defn send-task-failure
  ([task-token]
   (send-task-failure task-token nil))
  ([task-token {:keys [cause error] :as opts}]
   (send-task-failure (get-default-client) task-token opts))
  ([^AWSStepFunctionsClient client task-token {:keys [cause error]}]
   (->> (syms->map task-token cause error)
        mdl/map->SendTaskFailureRequest
        (.sendTaskFailure client))
   nil))

(defn send-task-success
  ([task-token output]
   (send-task-success (get-default-client) task-token output))
  ([^AWSStepFunctionsClient client task-token output]
   (->> (syms->map task-token output)
        mdl/map->SendTaskSuccessRequest
        (.sendTaskSuccess client))
   nil))

(defn send-task-heartbeat
  ([task-token]
   (send-task-heartbeat (get-default-client) task-token))
  ([^AWSStepFunctionsClient client task-token]
   (->> (syms->map task-token)
        mdl/map->SendTaskHeartbeatRequest
        (.sendTaskHeartbeat client))
   nil))

(defn start-execution
  ([state-machine-arn]
   (start-execution state-machine-arn nil))
  ([state-machine-arn {:keys [input name] :as opts}]
   (start-execution (get-default-client) state-machine-arn opts))
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
   (stop-execution (get-default-client) arn opts))
  ([^AWSStepFunctionsClient client arn {:keys [cause error]}]
   (->> (syms->map arn cause error)
        mdl/map->StopExecutionRequest
        (.stopExecution client))
   nil))

(defn auto-page' [client-fn items-key base-args xform]
  (let [items-chan (async/chan 1 xform)
        [base-pos-args base-map-args] (if (map? (last base-args))
                                        [(pop base-args) (last base-args)]
                                        [base-args {}])
        get-page   (fn [token]
                     (let [map-args (assoc base-map-args :next-token token)
                           args     (conj base-pos-args map-args)]
                       (apply client-fn args)))]
    (async/go
      (try (loop [page (get-page nil)]
             (async/<! (async/onto-chan items-chan
                                        (get page items-key)
                                        false))
             (when-let [next-token (::mdl/next-token page)]
               (recur (get-page next-token))))
           (catch Throwable e
             (async/>! items-chan e)))
      (async/close! items-chan))
    items-chan))

(def request-fn->items-key
  {#'get-execution-history ::mdl/events
   #'list-activities       ::mdl/activities
   #'list-executions       ::mdl/executions
   #'list-state-machines   ::mdl/state-machines})

(defmacro auto-page [request-form & [xform]]
  (let [request-fn-sym (first request-form)
        request-fn-var (resolve request-fn-sym)
        items-key      (request-fn->items-key request-fn-var)]
    (when-not items-key
      (throw (ex-info "Function called is not paginating"
                      {:request-fn request-fn-var})))
    `(auto-page' ~request-fn-sym
                 ~items-key
                 ~(vec (rest request-form))
                 ~xform)))

