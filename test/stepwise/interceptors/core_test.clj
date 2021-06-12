(ns stepwise.interceptors.core-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.test :refer [deftest testing is]]
            [stepwise.interceptors.core :as main]))

(def inc-x-interceptor-tuple
  [:inc-x
   {:enter (fn [ctx] (update-in ctx [:request :x] inc))}])

(defn handler [request]
  {:y (inc (:x request))})

(defn execute [queue task]
  ((main/compile queue handler) task (fn [])))

(deftest compile-test
  (testing "interceptor with handler-fn"
    (is (= {:y 42}
           (execute [inc-x-interceptor-tuple] {:x 40}))))

  (testing "send-heartbeat-fn is associated to internal context"
    (execute [[:check-heartbeat-fn
               {:enter (fn [ctx] (is (fn? (:send-heartbeat-fn ctx))) ctx)}]]
             {:x 40})))

