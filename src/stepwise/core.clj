(ns stepwise.core
  (:require [stepwise.client :as client]
            [stepwise.iam :as iam]
            [stepwise.sugar :as sgr]
            [stepwise.activities :as activities]
            [stepwise.workers :as workers]
            [stepwise.model :as mdl]
            [stepwise.arns :as arns]
            [clojure.set :as sets]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.walk :as walk]))

(defn denamespace-keys [response]
  (walk/prewalk (sgr/renamespace-keys (constantly true) nil)
                response))

(defn create-state-machine [name definition]
  (let [definition         (sgr/desugar definition)
        activity-name->arn (activities/ensure-all (activities/get-names definition))
        definition         (activities/resolve-kw-resources activity-name->arn definition)]
    (client/create-state-machine (arns/make-name name)
                                 definition
                                 (iam/ensure-execution-role))))

(defn describe-execution [arn]
  (-> (client/describe-execution arn)
      (denamespace-keys)))

(defn describe-state-machine [arn]
  (->> (update (client/describe-state-machine arn)
               ::mdl/definition
               sgr/sugar)
       (activities/resolve-resources arns/resource-arn->kw)
       (denamespace-keys)))

(defn start-execution
  ([state-machine-name]
   (start-execution state-machine-name nil))
  ([state-machine-name {:keys [input execution-name]}]
   (let [input (if execution-name
                 (assoc input :state-machine-name state-machine-name
                              :execution-name execution-name)
                 input)]
     (client/start-execution (arns/get-state-machine-arn state-machine-name)
                             {:input input
                              :name  execution-name}))))

(defn await-execution [execution-arn]
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

(defn run-execution
  ([state-machine-name]
   (run-execution state-machine-name nil))
  ([state-machine-name {:keys [input execution-name] :as opts}]
   (await-execution (start-execution state-machine-name opts))))

(defn start-workers
  ([task-handlers]
   (start-workers task-handlers nil))
  ([task-handlers {:keys [task-concurrency]}]
   (let [activity->arn (into {}
                             (map (fn [activity-name]
                                    [activity-name (arns/get-activity-arn activity-name)]))
                             (keys task-handlers))
         ; was tripping call throttles
         #_(activities/ensure-all (keys task-handlers))]
     (workers/boot (-> task-handlers
                       (sets/rename-keys activity->arn)
                       (activities/compile-all))
                   task-concurrency))))

(defn wait-all-exit! [{:keys [exit-chans]}]
  (->> (async/merge exit-chans)
       (async/into #{})
       (async/<!!)))

(defn shutdown-workers [workers]
  (async/>!! (:terminate-chan workers) :shutdown)
  (wait-all-exit! workers))

(defn kill-workers [workers]
  (async/>!! (:terminate-chan workers) :kill)
  (wait-all-exit! workers))

