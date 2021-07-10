(ns stepwise.interceptors.s3-offload)

(def stepwise-offload-tag :stepwise/offloaded-to-s3-arn)


(defn parse-s3-arns [m]
  (->> m
       (vals)
       (map #(get % stepwise-offload-tag))
       (filter identity)))

(defn payload-on-s3? [m]
  (-> (parse-s3-arns m)
      (not-empty)
      (boolean)))

(defn ensure-single-s3-arn [arns]
  (let [arns-set (into #{} arns)]
    (assert (= 1 (count arns-set))
            (format "Expecting only one ARN, but multiple S3 ARNs [%s] are parsed from request" arns))

    (first arns-set)))

(defn load-from-s3 [s3-client coll]
  ;; TODO
  )

(defn merge-request-with-offloaded-payload [s3-client request]
  (->> request
       (parse-s3-arns)
       (ensure-single-s3-arn)
       (load-from-s3 s3-client)
       (merge request)))


(defn offload-to-s3 [s3-client coll]
  ;; TODO
  )

