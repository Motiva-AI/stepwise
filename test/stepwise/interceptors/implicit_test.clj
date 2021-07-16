(ns stepwise.interceptors.implicit-test
  (:require [clojure.test :refer :all]
            [stepwise.interceptors.implicit :as ii]
            [stepwise.interceptors.s3-offload :as offload]))

(defn execute [interceptor ctx]
  ((:enter (interceptor)) ctx))

(def test-path "some-bucket/some-key")

(deftest load-from-s3-interceptor-fn-test
  (testing "no key is offloaded"
    (is (= {:request {:foo 3
                      :bar :soap}}
           (execute
             (partial ii/load-from-s3-interceptor (fn [_] {:bar :soap}))
             {:request {:foo 3
                        :bar :soap}}))))

  (testing "with some keys offloaded"
    (is (= {:request {:foo 3
                      :bar :soap}}
           (execute
             (partial ii/load-from-s3-interceptor (fn [_] {:bar :soap}))
             {:request {:foo 3
                        :bar (offload/stepwise-offloaded-map test-path)}}))))

  (testing "all keys offloaded"
    (is (= {:request {:foo 3
                      :bar :soap}}
           (execute
             (partial ii/load-from-s3-interceptor (fn [_] {:foo 3 :bar :soap}))
             {:request {:foo (offload/stepwise-offloaded-map test-path)
                        :bar (offload/stepwise-offloaded-map test-path)}})))))

