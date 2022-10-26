(ns stepwise.core-test
  (:require [clojure.test :refer :all]
            [stepwise.core :as sw]
            [stepwise.arns :as arns]
            [stepwise.client :as client]
            [clojure.data.json :as json]))

(def state-machine-keyword-name :stepwise-integration-test)
(def bucket-name "stepwise-integration-test")

(deftest ^:integration manipulate-task-input-and-output-test
  (when (is (sw/ensure-state-machine state-machine-keyword-name
                                     {:start-at :add
                                      :states   {:add {:type     :task
                                                       :resource :stepwise-integration-test/add
                                                       :comment  "unit test task"
                                                       :parameters {:comment "replaced comment"
                                                                    "MyInput" {"newX.$" "$.x"
                                                                               "newY.$" "$.y"}}
                                                       :result-selector (json/write-str {"newZ.$" "$.z"})
                                                       :result-path "$.add-output"
                                                       :heartbeat-seconds 3
                                                       :end      true}}}))

    (let [workers (sw/start-workers!
                    {:stepwise-integration-test/add (fn [{:keys [MyInput]}]
                                                      (let [{x :newX
                                                             y :newY} MyInput]
                                                        {:z (+ x y)}))})
          input   {:x 1 :y 1}]
      (is (= {:output (merge input {:add-output {:newZ 2}})
              :status "SUCCEEDED"}
             (as-> input $
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
