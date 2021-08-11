(ns stepwise.s3-test
  (:require [clojure.test :refer :all]
            [bond.james :as bond]
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

#_(deftest ^:integration invoke-or-exception-test
  (is (thrown-with-msg?
        Exception
        #"The specified bucket does not exist"
        (s3/invoke-or-exception
          (s3/get-s3-client)
          ;; missing required fields
          {:op :GetObject
           :request {:Bucket "NonExistBucket"
                     :Key "notfound"}}))))

;; TODO setup integration envs on CI
#_(deftest ^:integration offload-to-s3-round-trip-test
  (let [coll {:foo 3
              :bar :soap}
        key (s3/offload-to-s3 bucket-name coll)]
    (is (string? key))
    (is (re-seq (re-pattern bucket-name) key))
    (is (= coll (s3/load-from-s3 key)))))

(def test-path "MyBucket/some-key")

(deftest parse-s3-paths-test
  (is (nil? (s3/parse-s3-paths nil)))
  (is (nil? (s3/parse-s3-paths 3)))
  (is (empty? (s3/parse-s3-paths {})))
  (is (empty? (s3/parse-s3-paths {:foo nil})))
  (is (empty? (s3/parse-s3-paths {:foo :bar})))
  (is (= [test-path]
         (s3/parse-s3-paths {:foo (s3/stepwise-offloaded-map test-path)})))
  (is (= [test-path test-path]
         (s3/parse-s3-paths {:foo (s3/stepwise-offloaded-map test-path)
                                  :bar (s3/stepwise-offloaded-map test-path)}))))

(deftest payload-on-s3?-test
  (is (false? (s3/payload-on-s3? nil)))
  (is (false? (s3/payload-on-s3? 3)))
  (is (false? (s3/payload-on-s3? {})))
  (is (false? (s3/payload-on-s3? {:foo :bar})))
  (is (true? (s3/payload-on-s3? {:foo (s3/stepwise-offloaded-map test-path)})))
  (is (true? (s3/payload-on-s3? {:foo (s3/stepwise-offloaded-map test-path)
                                      :bar (s3/stepwise-offloaded-map test-path)}))))

(deftest ensure-single-s3-path-test
  (is (= test-path (s3/ensure-single-s3-path [test-path])))
  (is (= test-path (s3/ensure-single-s3-path [test-path test-path])))
  (is (thrown? java.lang.AssertionError (s3/ensure-single-s3-path [test-path test-path "some-other/path"]))))

(deftest large-size?-test
  (is (true? (s3/large-size? (repeat (int 1e6) :foo))))
  (is (false? (s3/large-size? {:foo :bar}))))

(deftest always-offload-select-keys-test
  (bond/with-stub! [[s3/offload-to-s3 (constantly test-path)]]
    (is (= {:x (s3/stepwise-offloaded-map test-path)
            :y (s3/stepwise-offloaded-map test-path)}
           (s3/always-offload-select-keys {:x 1 :y 1} [:x :y] test-path)))))

(deftest offload-select-keys-if-large-payload-test
  (bond/with-stub! [[s3/offload-to-s3 (constantly test-path)]]
    (is (= {:x 1 :y 1}
           (s3/offload-select-keys-if-large-payload {:x 1 :y 1} [:x :y] test-path)))))

