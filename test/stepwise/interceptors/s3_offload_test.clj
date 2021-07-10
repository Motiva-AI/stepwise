(ns stepwise.interceptors.s3-offload-test
  (:require [clojure.test :refer [deftest is]]
            [stepwise.interceptors.s3-offload :as offload]))

(def offloaded-map {offload/stepwise-offload-tag :this-is-an-arn})

(deftest parse-s3-arns-test
  (is (empty? (offload/parse-s3-arns {})))
  (is (empty? (offload/parse-s3-arns {:foo :bar})))
  (is (= [:this-is-an-arn]
         (offload/parse-s3-arns {:foo offloaded-map})))
  (is (= [:this-is-an-arn :this-is-an-arn]
         (offload/parse-s3-arns {:foo offloaded-map
                                 :bar offloaded-map}))))

(deftest payload-on-s3?-test
  (is (false? (offload/payload-on-s3? {})))
  (is (false? (offload/payload-on-s3? {:foo :bar})))
  (is (true? (offload/payload-on-s3? {:foo offloaded-map})))
  (is (true? (offload/payload-on-s3? {:foo offloaded-map
                                      :bar offloaded-map}))))

(deftest ensure-single-s3-arn-test
  (is (= :arn (offload/ensure-single-s3-arn [:arn])))
  (is (= :arn (offload/ensure-single-s3-arn [:arn :arn])))
  (is (thrown? java.lang.AssertionError (offload/ensure-single-s3-arn [:arn :arn :another-arn]))))
