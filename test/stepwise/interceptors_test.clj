(ns stepwise.interceptors-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.test :refer [deftest testing is]]
            [bond.james :as bond]
            [stepwise.interceptors :as i]
            [stepwise.interceptors.core :refer [well-formed-interceptor-tuple?]]
            [stepwise.client :as client]
            [stepwise.interceptors.s3-offload :as offload]
            [stepwise.interceptors.s3-offload-test :refer [offloaded-map]]))

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

(deftest load-from-s3-interceptor-fn-test
  (is (fn? (i/load-from-s3-interceptor-fn)))

  (bond/with-stub! [[client/load-from-s3 (fn [_] {:bar :soap})]]
    (testing "no key is offloaded"
      (is (= {:request {:foo 3
                        :bar :soap}}
             ((i/load-from-s3-interceptor-fn)
              {:request {:foo 3
                         :bar :soap}}))))

    (testing "with some keys offloaded"
      (is (= {:request {:foo 3
                        :bar :soap}}
             ((i/load-from-s3-interceptor-fn)
              {:request {:foo 3
                         :bar offloaded-map}})))))

  (testing "all keys offloaded"
    (bond/with-stub! [[client/load-from-s3 (fn [_] {:foo 3 :bar :soap})]]
      (is (= {:request {:foo 3
                        :bar :soap}}
             ((i/load-from-s3-interceptor-fn)
              {:request {:foo offloaded-map
                         :bar offloaded-map}}))))))

(defn offload-to-s3-mock [_] :this-is-an-arn)

(deftest offload-select-keys-to-s3-interceptor-fn-test
  (is (fn? (i/offload-select-keys-to-s3-interceptor-fn [:bar])))

  (bond/with-stub! [[client/offload-to-s3 offload-to-s3-mock]]
    (is (= {:response {:foo 3
                       :bar offloaded-map}}
           ((i/offload-select-keys-to-s3-interceptor-fn [:bar])
            {:response {:foo 3
                        :bar :soap}})))))

(deftest offload-all-keys-to-s3-interceptor-fn-test
  (is (fn? (i/offload-all-keys-to-s3-interceptor-fn)))

  (bond/with-stub! [[client/offload-to-s3 offload-to-s3-mock]]
    (is (= {:response {:foo offloaded-map
                       :bar offloaded-map}}
           ((i/offload-all-keys-to-s3-interceptor-fn)
            {:response {:foo 3
                        :bar :soap}})))))

