(ns stepwise.core-test
  (:require [clojure.test :refer :all]
            [stepwise.core :as sw]
            [stepwise.arns :as arns]
            [stepwise.client :as client]))

(def state-machine-keyword-name :stepwise-integration-test)
(def bucket-name "stepwise-integration-test")

(deftest ^:integration basic-state-machine-test
  (when (is (sw/ensure-state-machine state-machine-keyword-name
                                     {:start-at :add
                                      :states   {:add {:type     :task
                                                       :resource :stepwise-integration-test/add
                                                       :heartbeat-seconds 3
                                                       :end      true}}}))

    (let [workers (sw/start-workers!
                    {:stepwise-integration-test/add (fn [{:keys [x y]}] (+ x y)) })]

      (is (= {:output 2
              :status "SUCCEEDED"}
             (as-> {:x 1 :y 1} $
               (sw/offload-select-keys-to-s3 $ [:x :y] bucket-name)
               (hash-map :input $)
               (sw/start-execution!! :stepwise-integration-test $)
               (select-keys $ [:output :status]))))
      (is (= #{:done} (sw/shutdown-workers workers))))))

;; test fixture ;;;;;;;;;;;;;;

(defn- delete-state-machine [name]
  (let [name-ser (arns/make-name name)
        arn      (arns/get-state-machine-arn name-ser)]
    (client/delete-state-machine arn)
    ;; wait for state machine to be deleted on AWS
    (Thread/sleep 20000)))

(defn- state-machine-fixture [f]
  (f)
  (delete-state-machine state-machine-keyword-name))

(use-fixtures :once state-machine-fixture)

