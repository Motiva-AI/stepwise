(ns stepwise.activities-test
  (:require [clojure.test :as test]
            [stepwise.activities :as main]
            [clojure.core.async :as async]))

(defn boot [do-task-fn]
  (let [term-chan (async/chan)
        term-mult (async/mult term-chan)
        exit-chan (main/boot-worker
                    term-mult
                    "some-arn"
                    nil
                    (fn poll [_]
                      (async/go :task))
                    (fn handle [_ _]
                      (let [chan (async/chan)]
                        [chan
                         (future (do-task-fn :task)
                                 (async/>!! chan :done)
                                 (async/close! chan))])))]
    {:term-chan   term-chan
     :exited-chan exit-chan}))

(test/deftest boot-worker
  (test/testing "passes one task to handler and shuts down cleanly"
    (let [got-task (promise)
          {:keys [term-chan exited-chan]} (boot #(deliver got-task %))]
      (test/is (= :task
                  (deref got-task 10 :timeout)))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout 10)]))))
      (test/is (= :done
                  (first (async/alts!! [exited-chan
                                        (async/timeout 10)]))))))

  (test/testing "passes two tasks serially to handler and shuts down cleanly"
    (let [promise-a    (promise)
          promise-b    (promise)
          promise-chan (async/to-chan [promise-a promise-b])
          {:keys [term-chan exited-chan]} (boot (fn [task]
                                                  (when-let [prom (async/poll! promise-chan)]
                                                    (deliver prom task))))]
      (test/is (= :task
                  (deref promise-a 10 :timeout)))
      (test/is (= :task
                  (deref promise-b 10 :timeout)))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout 10)]))))
      (test/is (= :done
                  (first (async/alts!! [exited-chan
                                        (async/timeout 10)]))))))

  (test/testing "interrupts handler on kill"
    (let [got-exception       (promise)
          capture-interrupted (fn [_]
                                (try (Thread/sleep 2000)
                                     (catch InterruptedException e
                                       (deliver got-exception e))))
          {:keys [term-chan exited-chan]} (boot capture-interrupted)]
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :kill]
                                         (async/timeout 10)]))))
      (test/is (= :done
                  (first (async/alts!! [exited-chan
                                        (async/timeout 10)]))))
      (test/is (instance? InterruptedException
                          (deref got-exception 20 :timeout)))))

  (test/testing "interrupts handler that's holding up a shutdown on kill"
    (let [got-exception       (promise)
          capture-interrupted (fn [_]
                                (try (Thread/sleep 2000)
                                     (catch InterruptedException e
                                       (deliver got-exception e))))
          {:keys [term-chan exited-chan]} (boot capture-interrupted)]
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout 10)]))))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :kill]
                                         (async/timeout 10)]))))
      (test/is (= :done
                  (first (async/alts!! [exited-chan
                                        (async/timeout 10)]))))
      (test/is (instance? InterruptedException
                          (deref got-exception 20 :timeout))))))

