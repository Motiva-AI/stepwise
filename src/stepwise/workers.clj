(ns stepwise.workers
  (:require [clojure.core.async :as async]
            [stepwise.model :as mdl]
            [stepwise.client :as client]
            [stepwise.serialization :as ser]
            [clojure.string :as strs])
  (:import (com.amazonaws SdkClientException)
           (java.net SocketTimeoutException)))

(def default-activity-concurrency 1)

(defn poll [activity-arn]
  (let [chan (async/chan)]
    (future (async/>!! chan (try (client/get-activity-task activity-arn)
                                 (catch Throwable e
                                   (when-not (and (instance? SdkClientException e)
                                                  (instance? SocketTimeoutException (.getCause e)))
                                     ; TODO pluggable logging instead
                                     (prn e))
                                   e)))
            (async/close! chan))
    chan))

(defn exception->failure-map [^Throwable e]
  {:error (if-let [error (:error (ex-data e))]
            (ser/ser-error-val error)
            (or (not-empty (.getMessage e))
                "(See cause)"))
   :cause (ser/ser-exception e)})

(defn handle [task handler-fn]
  (let [chan (async/chan)]
    [chan (future (let [result (try (handler-fn (::mdl/input task)
                                                #(client/send-task-heartbeat
                                                   (::mdl/task-token task)))
                                    (catch Throwable e e))]
                    (when-not (.isInterrupted (Thread/currentThread))
                      (try
                        (if (instance? Throwable result)
                          (client/send-task-failure (::mdl/task-token task)
                                                    (exception->failure-map result))
                          (client/send-task-success (::mdl/task-token task)
                                                    result))
                        (catch Throwable e
                          ; TODO pluggable logging instead
                          (prn (ex-info "Failed to send activity task result"
                                        {:task   task
                                         :result result}
                                        e))))))
                  (async/>!! chan :done)
                  (async/close! chan))]))

(defn boot-worker
  ([terminate-mult activity-arn handler-fn]
   (boot-worker terminate-mult activity-arn handler-fn poll handle))
  ([terminate-mult activity-arn handler-fn poll handle]
   (let [terminate-chan (async/chan)]
     (async/tap terminate-mult terminate-chan)
     (async/go-loop [[message channel] (async/alts! [terminate-chan (poll activity-arn)])
                     handler-future nil
                     handler-chan nil
                     state ::polling]
       (condp = state
         ::polling
         (cond
           (= channel terminate-chan)
           :done

           (or (instance? Throwable message)
               (empty? message))
           (recur (async/alts! [terminate-chan (poll activity-arn)])
                  nil
                  nil
                  ::polling)

           :else
           (let [[handler-chan handler-future] (handle message handler-fn)]
             (recur (async/alts! [terminate-chan handler-chan])
                    handler-future
                    handler-chan
                    ::handling)))

         ::handling
         (cond
           (= channel terminate-chan)
           (if (= message :kill)
             ; Activity handling must be interruptable for this to have an immediate effect. Even if
             ; the handler continues, the task's completion won't be sent and it will eventually
             ; time out.
             (do (future-cancel handler-future)
                 :done)
             (recur (async/alts! [terminate-chan handler-chan])
                    handler-future
                    handler-chan
                    ::shutting-down))

           :else
           (recur (async/alts! [terminate-chan (poll activity-arn)])
                  nil
                  nil
                  ::polling))

         ::shutting-down
         (cond
           (and (= channel terminate-chan)
                (= message :kill))
           (do (future-cancel handler-future)
               :done)

           :else :done))))))

(defn boot-workers [terminate-mult activity-arn handler-fn concurrency]
  (into #{}
        (map (fn [_]
               (boot-worker terminate-mult activity-arn handler-fn)))
        (range 0 concurrency)))

(defn boot [activity-arn->handler-fn & [activity-arn->concurrency]]
  (let [terminate-chan (async/chan)
        terminate-mult (async/mult terminate-chan)
        exited-chans   (into #{}
                             (mapcat (fn [[activity-arn handler-fn]]
                                       (boot-workers terminate-mult
                                                     activity-arn
                                                     handler-fn
                                                     (get activity-arn->concurrency
                                                          activity-arn
                                                          default-activity-concurrency))))
                             activity-arn->handler-fn)
        exited-chan    (->> exited-chans
                            async/merge
                            (async/into #{}))]
    {:terminate-chan terminate-chan
     :exited-chan    exited-chan}))

