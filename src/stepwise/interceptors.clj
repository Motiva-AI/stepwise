(ns stepwise.interceptors
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [stepwise.interceptors.s3_offload :as offload]))

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

(defn- map-vals [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn load-from-s3-interceptor-fn [s3-client]
  (fn [ctx]
    (->> #(map-vals % (partial offload/load-from-s3 s3-client))
         (update ctx :request))))

(defn- offload-values-to-s3 [s3-client m]
  (map-vals m (partial offload/offload-to-s3 s3-client)))

(defn offload-select-keys-to-s3-interceptor-fn [s3-client keyseq]
  (fn [{response :response :as ctx}]
    (->> (select-keys response keyseq)
         (offload-values-to-s3 s3-client)
         (merge response)
         (assoc ctx :response))))

(defn offload-all-keys-to-s3-interceptor-fn [s3-client]
  (fn [{response :response :as ctx}]
    (->> response
         (offload-values-to-s3 s3-client)
         (merge response)
         (assoc ctx :response))))

