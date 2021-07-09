(ns stepwise.interceptors-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.test :refer [deftest testing is]]
            [bond.james :as bond]
            [stepwise.interceptors :as i]
            [stepwise.interceptors.core :refer [well-formed-interceptor-tuple?]]
            [stepwise.interceptors.s3_offload :as offload]))

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

(def s3-client-stub :s3-client-stub)

(deftest load-from-s3-interceptor-fn-test
  (is (fn? (i/load-from-s3-interceptor-fn s3-client-stub)))

  (bond/with-stub! [[offload/load-from-s3
                     (fn [_ coll]
                       ;; TODO refactor this into s3-offload ns
                       (if (offload/payload-on-s3? coll)
                         (-> coll
                             (get offload/stepwise-offload-tag)
                             (name))
                         coll))]]
    (is (= {:request {:foo 3
                      :bar "this-is-an-arn"}}
           ((i/load-from-s3-interceptor-fn s3-client-stub)
            {:request {:foo 3
                       :bar {offload/stepwise-offload-tag :this-is-an-arn}}})))))

(defn offload-to-s3-mock [_ coll]
  {offload/stepwise-offload-tag (keyword coll)})

(deftest offload-select-keys-to-s3-interceptor-fn-test
  (is (fn? (i/offload-select-keys-to-s3-interceptor-fn s3-client-stub [:bar])))

  (bond/with-stub! [[offload/offload-to-s3 offload-to-s3-mock]]
    (is (= {:response {:foo 3
                       :bar {offload/stepwise-offload-tag :this-is-an-arn}}}
           ((i/offload-select-keys-to-s3-interceptor-fn s3-client-stub [:bar])
            {:response {:foo 3
                        :bar "this-is-an-arn"}})))))

(deftest offload-all-keys-to-s3-interceptor-fn-test
  (is (fn? (i/offload-all-keys-to-s3-interceptor-fn s3-client-stub)))

  (bond/with-stub! [[offload/offload-to-s3 offload-to-s3-mock]]
    (is (= {:response {:foo {offload/stepwise-offload-tag :another-arn}
                       :bar {offload/stepwise-offload-tag :this-is-an-arn}}}
           ((i/offload-all-keys-to-s3-interceptor-fn s3-client-stub)
            {:response {:foo "another-arn"
                        :bar "this-is-an-arn"}})))))

