(ns stepwise.interceptors
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [stepwise.interceptors.s3-offload :as offload]
            [stepwise.s3 :as s3]))

;; Send Heartbeat

(defn- safely-called-heartbeat-fn?
  "Evaluate heartbeat-fn in a try-catch. Returns false on any Exception
   thrown. Otherwise, returns true."
  [heartbeat-fn]
  (try
    (heartbeat-fn)
    ;; happy path return
    true

    (catch com.amazonaws.services.stepfunctions.model.TaskTimedOutException _
      ; When the corresponding handler-fn returns, meaning
      ; job is done so heartbeat ping is no longer needed,
      ; the message token closed over this heartbeat-fn would
      ; expire and calling (heartbeat-fn) would throw
      ; `com.amazonaws.services.stepfunctions.model.TaskTimedOutException`.
      ; This catches that and exits the loop gracefully
      (log/debugf "Stopping periodic heartbeat ping because task is completed.")
      false)
    (catch Exception ex
      (log/debugf "Stopping periodic heartbeat ping due to unexpected error: %s." (.getMessage ex))
      false)))

(defn beat-heart-every-n-seconds!
  "Calls heartbeat-fn every n seconds. Stops the loop after any Exception thrown by heartbeat-fn."
  [heartbeat-fn n-seconds]
  {:pre [(fn? heartbeat-fn)]}
  (log/debug "Starting periodic heartbeat ping.")

  (async/go-loop []
    (async/<! (async/timeout (* n-seconds 1000)))
    (when (safely-called-heartbeat-fn? heartbeat-fn)
      (recur))))

(defn send-heartbeat-every-n-seconds-interceptor
  "Calls send-heartbeat-fn every n seconds. send-heartbeat-fn is provided internally by Stepwise."
  [n-seconds]
  [:send-heartbeat
   {:enter
    (fn [{send-heartbeat-fn :send-heartbeat-fn
          :as ctx}]
      (beat-heart-every-n-seconds! send-heartbeat-fn n-seconds)
      ctx)}])

;; Offload Payload

(defn load-from-s3-interceptor-fn []
  (fn [{request :request :as ctx}]
    (if (offload/payload-on-s3? request)
      (update ctx :request (partial offload/merge-request-with-offloaded-payload s3/load-from-s3))

      ;; nothing was offloaded
      ctx)))

(defn offload-select-keys-to-s3-interceptor-fn [s3-bucket-name keyseq]
  (fn [{response :response :as ctx}]
    (->> (select-keys response keyseq)
         (offload/replace-vals-with-offloaded-s3-path (partial s3/offload-to-s3 s3-bucket-name))
         (merge response)
         (assoc ctx :response))))

(defn offload-all-keys-to-s3-interceptor-fn [s3-bucket-name]
  (fn [ctx]
    (update ctx :response (partial offload/replace-vals-with-offloaded-s3-path (partial s3/offload-to-s3 s3-bucket-name)))))

