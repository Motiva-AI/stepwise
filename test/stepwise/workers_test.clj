(ns stepwise.workers-test
  (:require [clojure.test :as test]
            [stepwise.workers :as main]
            [clojure.core.async :as async]))

(def wait-timeout-ms 500)

(defn boot
  ([do-task-fn]
   (boot do-task-fn
         (fn poll [_]
           (async/go :input))))
  ([do-task-fn poll]
   (let [term-chan       (async/chan)
         term-mult       (async/mult term-chan)
         all-exited-chan (main/boot-worker
                           term-mult
                           "some-arn"
                           nil
                           poll
                           (fn handle [_ _ _]
                             (let [chan (async/chan)]
                               [chan
                                (future (do-task-fn :input)
                                        (async/>!! chan :done)
                                        (async/close! chan))])))]
     {:term-chan       term-chan
      :all-exited-chan all-exited-chan})))

(test/deftest boot-worker
  (test/testing "passes one task to handler and shuts down cleanly"
    (let [got-task (promise)
          {:keys [term-chan all-exited-chan]} (boot #(deliver got-task %))]
      (test/is (= :input
                  (deref got-task wait-timeout-ms :timeout)))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout wait-timeout-ms)]))))
      (test/is (= :done
                  (first (async/alts!! [all-exited-chan
                                        (async/timeout wait-timeout-ms)]))))))

  (test/testing "passes two tasks serially to handler and shuts down cleanly"
    (let [promise-a    (promise)
          promise-b    (promise)
          promise-chan (async/to-chan [promise-a promise-b])
          {:keys [term-chan all-exited-chan]} (boot (fn [task]
                                                      (when-let [prom (async/poll! promise-chan)]
                                                        (deliver prom task))))]
      (test/is (= :input
                  (deref promise-a wait-timeout-ms :timeout)))
      (test/is (= :input
                  (deref promise-b wait-timeout-ms :timeout)))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout wait-timeout-ms)]))))
      (test/is (= :done
                  (first (async/alts!! [all-exited-chan
                                        (async/timeout wait-timeout-ms)]))))))

  (test/testing "interrupts handler on kill"
    (let [got-exception       (promise)
          capture-interrupted (fn [_]
                                (try (Thread/sleep (* wait-timeout-ms 3))
                                     (catch InterruptedException e
                                       (deliver got-exception e))))
          {:keys [term-chan all-exited-chan]} (boot capture-interrupted)]
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :kill]
                                         (async/timeout wait-timeout-ms)]))))
      (test/is (= :done
                  (first (async/alts!! [all-exited-chan
                                        (async/timeout wait-timeout-ms)]))))
      (test/is (instance? InterruptedException
                          (deref got-exception wait-timeout-ms :timeout)))))

  (test/testing "interrupts handler that's holding up a shutdown on kill"
    (let [got-exception       (promise)
          capture-interrupted (fn [_]
                                (try (Thread/sleep (* wait-timeout-ms 4))
                                     (catch InterruptedException e
                                       (deliver got-exception e))))
          {:keys [term-chan all-exited-chan]} (boot capture-interrupted)]
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout wait-timeout-ms)]))))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :kill]
                                         (async/timeout wait-timeout-ms)]))))
      (test/is (= :done
                  (first (async/alts!! [all-exited-chan
                                        (async/timeout wait-timeout-ms)]))))
      (test/is (instance? InterruptedException
                          (deref got-exception wait-timeout-ms :timeout))))))

; TODO coverage for polling backoff
; TODO coverage for worker death on missing activity

{:output            2,
 :state-machine-arn "arn:aws:states:us-west-2:256212633204:stateMachine:adder",
 :start-date        #inst"2017-06-20T22:48:14.241-00:00",
 :stop-date         #inst"2017-06-20T22:48:14.425-00:00",
 :input             {:x 1, :y 1},
 :arn               "arn:aws:states:us-west-2:256212633204:execution:adder:9c5623c6-eee3-49fa-a7d6-22e3ca236c9a",
 :status            "SUCCEEDED",
 :name              "9c5623c6-eee3-49fa-a7d6-22e3ca236c9a"}
