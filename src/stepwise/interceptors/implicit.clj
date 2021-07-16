(ns stepwise.interceptors.implicit
  (:require [stepwise.interceptors.s3-offload :as offload]))

(defn assoc-send-heartbeat-fn-to-context-interceptor
  [send-heartbeat-fn]
  {:enter (fn [ctx] (assoc ctx :send-heartbeat-fn send-heartbeat-fn))})

(defn load-from-s3-interceptor [load-from-s3-fn]
  {:enter
   (fn [{request :request :as ctx}]
     (if (offload/payload-on-s3? request)
       (update ctx :request (partial offload/merge-request-with-offloaded-payload
                                     load-from-s3-fn))

       ;; nothing was offloaded
       ctx))})


