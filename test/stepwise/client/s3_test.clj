(ns stepwise.client.s3-test
  (:require [clojure.test :refer [deftest is]]
            [stepwise.client.s3 :as s3]

            [clojure.java.io]))

(deftest parse-s3-bucket-and-key-from-arn-test
  (is (= {} (s3/parse-s3-bucket-and-key-from-arn "")))
  (is (= {:Bucket "MyBucket", :Key "data.json"}
         (s3/parse-s3-bucket-and-key-from-arn "arn:aws:s3:::MyBucket/data.json"))))

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

