(ns stepwise.interceptors
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]))

(defn beat-heart-every-n-seconds!
  "Calls heartbeat-fn every n seconds. Stops the loop after any Exception thrown by heartbeat-fn."
  [heartbeat-fn n-seconds]
  (log/debug "Starting periodic heartbeat ping.")

  (async/go-loop
    []
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
              (log/debugf "Stopping periodic heartbeat ping.")
              false))]
      (when continue? (recur)))))

(defn send-heartbeat-interceptor
  "Usage:

   (stepwise/start-workers!

   ::addr
   {:handler-fn   add
    :interceptors [(send-heartbeat-interceptor 5) [...] [...] ...]}
   "
  [n-seconds]
  [:send-heartbeat
   {:enter
    (fn [{send-heartbeat-fn :send-heartbeat-fn
          :as ctx}]
      (beat-heart-every-n-seconds! (send-heartbeat-fn) n-seconds)
      ctx)}])

