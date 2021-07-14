(ns stepwise.interceptors.s3-offload)

(def stepwise-offload-tag :stepwise/offloaded-to-s3)
(defn stepwise-offloaded-map [s3-path]
  {stepwise-offload-tag s3-path})

(defn parse-s3-paths [m]
  (->> m
       (vals)
       (map #(get % stepwise-offload-tag))
       (filter identity)))

(defn payload-on-s3? [m]
  (-> (parse-s3-paths m)
      (not-empty)
      (boolean)))

(defn ensure-single-s3-path [paths]
  (let [paths-set (into #{} paths)]
    (assert (= 1 (count paths-set))
            (format "Expecting only one distinct path, but multiple different S3 paths #{%s} are parsed from request" paths-set))

    (first paths-set)))

(defn merge-request-with-offloaded-payload [load-from-s3-fn request]
  (->> request
       (parse-s3-paths)
       ;; TODO enable loading from multiple s3 paths. Use case: if the
       ;; payload is offloaded in different activities and merged together via
       ;; Step Function paths
       (ensure-single-s3-path)
       (load-from-s3-fn)
       (merge request)))

(defn- replace-vals [m v]
  (into {} (for [[k _] m] [k v])))

(defn replace-vals-with-offloaded-s3-path [offload-to-s3-fn m]
  (let [s3-path (offload-to-s3-fn m)]
    (replace-vals m (stepwise-offloaded-map s3-path))))

