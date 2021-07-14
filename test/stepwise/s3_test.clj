(ns stepwise.s3-test
  (:require [clojure.test :refer :all]
            [stepwise.s3 :as s3]))

(def bucket-name "stepwise-integration-test")


(deftest parse-s3-bucket-and-key-test
  (is (= {} (s3/parse-s3-bucket-and-key "")))
  (is (nil? (s3/unparse-s3-bucket-and-key {})))

  (let [m {:Bucket "MyBucket", :Key "data.json"}
        expected-str (str "MyBucket" s3/bucket-key-separator "data.json")]
    (is (= expected-str (s3/unparse-s3-bucket-and-key m)))
    (is (= m (s3/parse-s3-bucket-and-key expected-str)))))

(deftest slurp-bytes-test
  (let [msg "test 123"]
    (is (= msg
           (-> msg
               (.getBytes)
               (s3/slurp-bytes)
               (String.))))))

(deftest ser-de-round-trip-test
  (let [msg "test 123"
        ser (s3/serialize msg)]
    (is (bytes? ser))
    (is (= msg (s3/deseralize ser)))))

;; TODO setup integration envs on CI
#_(deftest ^:integration offload-to-s3-round-trip-test
  (let [coll {:foo 3
              :bar :soap}
        key (s3/offload-to-s3 bucket-name coll)]
    (is (string? key))
    (is (= coll (s3/load-from-s3 (str bucket-name s3/bucket-key-separator key))))))

