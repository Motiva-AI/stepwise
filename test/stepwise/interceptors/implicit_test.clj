(ns stepwise.interceptors.implicit-test
  (:require [clojure.test :refer :all]
            [bond.james :as bond]
            [stepwise.interceptors.implicit :as ii]
            [stepwise.s3 :as s3]))

(defn execute [interceptor ctx]
  ((:enter (interceptor)) ctx))

(def test-path "some-bucket/some-key")

(deftest load-from-s3-interceptor-fn-test
  (bond/with-stub! [[s3/load-from-s3 (fn [_] {:bar :soap})]]
    (testing "no key is offloaded"
      (is (= {:request {:foo 3
                        :bar :soap}}
             (execute
               (partial ii/load-from-s3-interceptor)
               {:request {:foo 3
                          :bar :soap}}))))

    (testing "with some keys offloaded"
      (is (= {:request {:foo 3
                        :bar :soap}}
             (execute
               (partial ii/load-from-s3-interceptor)
               {:request {:foo 3
                          :bar (s3/stepwise-offloaded-map test-path)}})))))

  (testing "all keys offloaded"
    (bond/with-stub! [[s3/load-from-s3 (fn [_] {:foo 3 :bar :soap})]]
      (is (= {:request {:foo 3
                        :bar :soap}}
             (execute
               (partial ii/load-from-s3-interceptor)
               {:request {:foo (s3/stepwise-offloaded-map test-path)
                          :bar (s3/stepwise-offloaded-map test-path)}}))))))

