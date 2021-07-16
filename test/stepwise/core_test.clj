(ns stepwise.core-test
  (:require [clojure.test :refer :all]
            [stepwise.core :as sw]
            [stepwise.interceptors.s3-offload :as offload]
            [stepwise.s3 :as s3]))

#_(deftest ^:integration adder-state-machine-test
  (is (sw/ensure-state-machine :adder
                               {:start-at :add
                                :states   {:add {:type     :task
                                                 :resource :activity/add
                                                 :heartbeat-seconds 3
                                                 :end      true}}}))

  (let [body
        ;; TODO refactor this out
        (offload/replace-vals-with-offloaded-s3-path
          (partial s3/offload-to-s3 "stepwise-integration-test")
          {:x 1 :y 1})

        workers (sw/start-workers!
                  {:activity/add (fn [{:keys [x y]}] (+ x y)) })]

    (is (= {:output 2
            :status "SUCCEEDED"}
           (-> (sw/start-execution!! :adder {:input body})
               (select-keys [:output :status]))))
    (is (= #{:done} (sw/shutdown-workers workers)))))
