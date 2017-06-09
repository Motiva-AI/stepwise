(ns stepwise.interceptors-test
  (:require [clojure.test :as test]
            [stepwise.interceptors :as main]))

(def hello-world
  [:hello-world {:before (fn [context]
                           (assoc context :output :hello-world))}])

(defn execute [queue task]
  ((main/compile queue) task (fn [])))

(test/deftest compile
  (test/is (= :hello-world
              (execute [hello-world] {}))))

