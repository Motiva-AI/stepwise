(ns stepwise.workers-test
  (:require [clojure.test :as test]
            [stepwise.workers :as main]
            [clojure.core.async :as async]))

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
                  (deref got-task 100 :timeout)))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout 10)]))))
      (test/is (= :done
                  (first (async/alts!! [all-exited-chan
                                        (async/timeout 10)]))))))

  (test/testing "passes two tasks serially to handler and shuts down cleanly"
    (let [promise-a    (promise)
          promise-b    (promise)
          promise-chan (async/to-chan [promise-a promise-b])
          {:keys [term-chan all-exited-chan]} (boot (fn [task]
                                                      (when-let [prom (async/poll! promise-chan)]
                                                        (deliver prom task))))]
      (test/is (= :input
                  (deref promise-a 10 :timeout)))
      (test/is (= :input
                  (deref promise-b 10 :timeout)))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout 10)]))))
      (test/is (= :done
                  (first (async/alts!! [all-exited-chan
                                        (async/timeout 10)]))))))

  (test/testing "interrupts handler on kill"
    (let [got-exception       (promise)
          capture-interrupted (fn [_]
                                (try (Thread/sleep 2000)
                                     (catch InterruptedException e
                                       (deliver got-exception e))))
          {:keys [term-chan all-exited-chan]} (boot capture-interrupted)]
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :kill]
                                         (async/timeout 100)]))))
      (test/is (= :done
                  (first (async/alts!! [all-exited-chan
                                        (async/timeout 100)]))))
      (test/is (instance? InterruptedException
                          (deref got-exception 200 :timeout)))))

  (test/testing "interrupts handler that's holding up a shutdown on kill"
    (let [got-exception       (promise)
          capture-interrupted (fn [_]
                                (try (Thread/sleep 2000)
                                     (catch InterruptedException e
                                       (deliver got-exception e))))
          {:keys [term-chan all-exited-chan]} (boot capture-interrupted)]
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :shutdown]
                                         (async/timeout 100)]))))
      (test/is (= term-chan
                  (second (async/alts!! [[term-chan :kill]
                                         (async/timeout 100)]))))
      (test/is (= :done
                  (first (async/alts!! [all-exited-chan
                                        (async/timeout 100)]))))
      (test/is (instance? InterruptedException
                          (deref got-exception 100 :timeout))))))

; TODO coverage for polling backoff
; TODO coverage for worker death on missing activity

