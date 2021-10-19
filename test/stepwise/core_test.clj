(ns stepwise.core-test
  (:require [clojure.test :refer :all]
            [stepwise.core :as sw]))

(def bucket-name "stepwise-integration-test")

(deftest ^:integration adder-state-machine-test
  (is (sw/ensure-state-machine :adder
                               {:start-at :add
                                :states   {:add {:type     :task
                                                 :resource :activity/add
                                                 :heartbeat-seconds 3
                                                 :end      true}}}))

  (let [workers (sw/start-workers!
                  {:activity/add (fn [{:keys [x y]}] (+ x y)) })]

    (is (= {:output 2
            :status "SUCCEEDED"}
           (as-> {:x 1 :y 1} $
             (sw/offload-select-keys-to-s3 $ [:x :y] bucket-name)
             (hash-map :input $)
             (sw/start-execution!! :adder $)
             (select-keys $ [:output :status]))))
    (is (= #{:done} (sw/shutdown-workers workers)))))

