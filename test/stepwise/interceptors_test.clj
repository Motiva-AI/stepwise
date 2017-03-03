(ns stepwise.interceptors-test
  (:require [clojure.test :as test]
            [stepwise.interceptors :as main]))

(def hello-world
  {:id     :hello-world
   :before (fn [context]
             (assoc context :result :hello-world))})

(defn execute [queue task]
  ((main/compile queue) task))

(test/deftest compile
  (test/is (= :hello-world
              (execute [hello-world] {}))))

