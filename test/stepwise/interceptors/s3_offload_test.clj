(ns stepwise.interceptors.s3-offload-test
  (:require [clojure.test :refer [deftest is]]
            [stepwise.interceptors.s3-offload :as offload]))

(def test-path "MyBucket/some-key")

(deftest parse-s3-paths-test
  (is (empty? (offload/parse-s3-paths {})))
  (is (empty? (offload/parse-s3-paths {:foo :bar})))
  (is (= [test-path]
         (offload/parse-s3-paths {:foo (offload/stepwise-offloaded-map test-path)})))
  (is (= [test-path test-path]
         (offload/parse-s3-paths {:foo (offload/stepwise-offloaded-map test-path)
                                  :bar (offload/stepwise-offloaded-map test-path)}))))

(deftest payload-on-s3?-test
  (is (false? (offload/payload-on-s3? {})))
  (is (false? (offload/payload-on-s3? {:foo :bar})))
  (is (true? (offload/payload-on-s3? {:foo (offload/stepwise-offloaded-map test-path)})))
  (is (true? (offload/payload-on-s3? {:foo (offload/stepwise-offloaded-map test-path)
                                      :bar (offload/stepwise-offloaded-map test-path)}))))

(deftest ensure-single-s3-path-test
  (is (= test-path (offload/ensure-single-s3-path [test-path])))
  (is (= test-path (offload/ensure-single-s3-path [test-path test-path])))
  (is (thrown? java.lang.AssertionError (offload/ensure-single-s3-path [test-path test-path "some-other/path"]))))
