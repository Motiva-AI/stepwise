(ns stepwise.interceptors.implicit
  (:require [stepwise.s3 :as s3]))

(defn assoc-send-heartbeat-fn-to-context-interceptor
  [send-heartbeat-fn]
  {:enter (fn [ctx] (assoc ctx :send-heartbeat-fn send-heartbeat-fn))})

(defn load-from-s3-interceptor []
  {:enter
   (fn [{request :request :as ctx}]
     (if (s3/payload-on-s3? request)
       (update ctx :request s3/merge-request-with-offloaded-payload)

       ;; nothing was offloaded
       ctx))})


