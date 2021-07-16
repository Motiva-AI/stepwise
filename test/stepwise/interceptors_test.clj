(ns stepwise.interceptors-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.test :refer [deftest testing is]]
            [bond.james :as bond]
            [stepwise.interceptors :as i]
            [stepwise.interceptors.core :refer [well-formed-interceptor-tuple?]]
            [stepwise.s3 :as s3]))

(defn- heartbeat-fn [] :foo)
(defn- failing-heartbeat-fn [] (throw (Exception. "testing failure mode")))

(deftest beat-heart-every-n-seconds!-test
  (let [n          2
        period-sec 1]
    (testing "heartbeat-fn is called number of times equal to time period elapsed"
      (bond/with-spy [heartbeat-fn]
        (is (i/beat-heart-every-n-seconds! heartbeat-fn 1))
        (Thread/sleep (* (inc n) period-sec 1000))
        (is (= n (-> heartbeat-fn bond/calls count)))))


    (testing "loop is exited gracefully after exception is thrown"
      (bond/with-spy [failing-heartbeat-fn]
        (is (i/beat-heart-every-n-seconds! failing-heartbeat-fn 1))
        (Thread/sleep (* (inc n) period-sec 1000))
        (is (= 1 (-> failing-heartbeat-fn bond/calls count)))))))

(deftest send-heartbeat-every-n-seconds-interceptor-test
  (is (well-formed-interceptor-tuple? (i/send-heartbeat-every-n-seconds-interceptor 5))))

(def test-path "some-bucket/some-key")

(def offload-to-s3-mock (constantly test-path))
(def bucket-name "some-bucket-name")

(deftest offload-select-keys-to-s3-interceptor-fn-test
  (is (fn? (i/offload-select-keys-to-s3-interceptor-fn bucket-name [:bar])))

  (bond/with-stub! [[s3/offload-to-s3 offload-to-s3-mock]]
    (is (= {:response {:foo 3
                       :bar (s3/stepwise-offloaded-map test-path)}}
           ((i/offload-select-keys-to-s3-interceptor-fn bucket-name [:bar])
            {:response {:foo 3
                        :bar :soap}})))))

(deftest offload-all-keys-to-s3-interceptor-fn-test
  (is (fn? (i/offload-all-keys-to-s3-interceptor-fn bucket-name)))

  (bond/with-stub! [[s3/offload-to-s3 offload-to-s3-mock]]
    (is (= {:response {:foo (s3/stepwise-offloaded-map test-path)
                       :bar (s3/stepwise-offloaded-map test-path)}}
           ((i/offload-all-keys-to-s3-interceptor-fn bucket-name)
            {:response {:foo 3
                        :bar :soap}})))))

