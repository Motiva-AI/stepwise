(ns stepwise.interceptors.s3_offload)

(def stepwise-offload-tag :stepwise/offloaded-to-s3-arn)

(defn payload-on-s3? [coll]
  (get coll stepwise-offload-tag false))

(defn load-from-s3 [s3-client coll]
  ;; TODO
  )

(defn offload-to-s3 [s3-client coll]
  ;; TODO
  )

