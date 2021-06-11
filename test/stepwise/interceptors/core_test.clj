(ns stepwise.interceptors.core-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.test :as test]
            [stepwise.interceptors.core :as main]))

(def inc-x-interceptor-tuple
  [:inc-x
   {:enter (fn [ctx] (update-in ctx [:request :x] inc))}])

(defn handler [request]
  {:y (inc (:x request))})

(defn execute [queue task]
  ((main/compile queue handler) task (fn [])))

(test/deftest compile-test
  (test/is (= {:y 42}
              (execute [inc-x-interceptor-tuple] {:x 40}))))

