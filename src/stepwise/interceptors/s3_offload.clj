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

(defn merge-request-with-offloaded-payload [load-from-s3-fn request]
  (->> request
       (parse-s3-arns)
       (ensure-single-s3-arn)
       (load-from-s3-fn)
       (merge request)))

(defn- replace-vals [m v]
  (into {} (for [[k _] m] [k v])))

(defn replace-vals-with-offloaded-s3-arn [offload-to-s3-fn m]
  (let [s3-arn (offload-to-s3-fn m)]
    (replace-vals m {stepwise-offload-tag s3-arn})))

