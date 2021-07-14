(ns stepwise.client.s3-test
  (:require [clojure.test :refer [deftest is]]
            [stepwise.client.s3 :as s3]

            [clojure.java.io]))

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

