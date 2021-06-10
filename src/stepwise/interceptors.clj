(ns stepwise.interceptors
  (:require [clojure.core.async :as async]))

(defn beat-heart-every-n-seconds!
  "Calls heartbeat-fn every n seconds. Stops the loop after any Exception thrown by heartbeat-fn."
  [heartbeat-fn n-seconds]
  (async/go-loop []
    (async/<! (async/timeout (* n-seconds 1000)))
    (let [continue?
          (try
            (heartbeat-fn)
            ;; happy path return
            true

            (catch Exception _
              ; When the corresponding handler-fn returns, meaning
              ; job is done so heartbeat ping is no longer needed,
              ; the message token closed over this heartbeat-fn would
              ; expire and calling (heartbeat-fn) would throw
              ; `com.amazonaws.services.stepfunctions.model.TaskTimedOutException`.
              ; This catches that and exits the loop gracefully
              false))]
      (when continue? (recur)))))

(defn send-heartbeat-interceptor-fn
  "Usage:

   (stepwise/start-workers!

   ::addr
   {:handler-fn   add
    :interceptors [[:send-heartbeat
                    {:before (send-heartbeat-interceptor-fn 10)}]]}
   "
  [n-seconds]
  (fn [{{heartbeat-fn :send-heartbeat} :context
        :as env}]
    (beat-heart-every-n-seconds! (heartbeat-fn) n-seconds)
    env))

