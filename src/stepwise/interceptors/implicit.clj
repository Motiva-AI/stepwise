(ns stepwise.interceptors.implicit
  (:require [stepwise.interceptors.s3-offload :as offload]
            ;; should not be calling a higher level ns
            [stepwise.s3 :as s3]))

(defn assoc-send-heartbeat-fn-to-context-interceptor
  [send-heartbeat-fn]
  {:enter (fn [ctx] (assoc ctx :send-heartbeat-fn send-heartbeat-fn))})

(defn load-from-s3-interceptor []
  {:enter
   (fn [{request :request :as ctx}]
     (if (offload/payload-on-s3? request)
       (update ctx :request (partial offload/merge-request-with-offloaded-payload
                                     ;; TODO this is reaching across weirdness to a distant ns
                                     s3/load-from-s3))

       ;; nothing was offloaded
       ctx))})


