(ns stepwise.workers
  (:require [clojure.core.async :as async]
            [stepwise.model :as mdl]
            [stepwise.client :as client]
            [stepwise.serialization :as ser]
            [clojure.tools.logging :as log]
            [stepwise.maps :as maps])
  (:import (com.amazonaws AmazonServiceException)))

(def default-activity-concurrency 1)
(def max-error-length 256)
(def max-cause-length 32768)

(defn poll [activity-arn]
  (let [chan (async/chan)]
    (future (log/debug "Polling for activity task"
                       (prn-str {:arn activity-arn}))
            (async/>!! chan (try (client/get-activity-task activity-arn)
                                 (catch Throwable e
                                   (log/warn (prn-str e))
                                   e)))
            (async/close! chan))
    chan))

(defn truncate [string max-length]
  (if (> (count string) max-length)
    (subs string 0 max-length)
    string))

(defn exception->failure-map [^Throwable e]
  {:error (truncate (if-let [error (:error (ex-data e))]
                      (ser/ser-error-val error)
                      (or (not-empty (.getMessage e))
                          "(See cause)"))
                    max-error-length)
   :cause (if (get (ex-data e) :include-cause? true)
            (truncate (ser/ser-exception e)
                      max-cause-length)
            "")})

(defn- handler-fn-failed [activity-arn task error]
  (log/info error
            "Activity task failed"
            (prn-str (maps/syms->map activity-arn task)))
  (client/send-task-failure (::mdl/task-token task)
                            (exception->failure-map error)))

(defn- handler-fn-succeeded [activity-arn task result]
  (log/info "Activity task completed"
            (prn-str (maps/syms->map activity-arn task result)))
  (client/send-task-success (::mdl/task-token task)
                            result))

(defn- handle-result [activity-arn task result]
  (try
    (if (instance? Throwable result)
      (handler-fn-failed activity-arn task result)
      (handler-fn-succeeded activity-arn task result))

    (catch Throwable e
      (log/error e
                 "Failed to send activity task result"
                 (prn-str (maps/syms->map activity-arn task result))))))

(defn handle [activity-arn task handler-fn]
  (let [chan (async/chan)]
    [chan (future (log/debug "Handling activity task"
                             {:arn  activity-arn
                              :task task})

                  (when-not (.isInterrupted (Thread/currentThread))
                    (let [result (try (handler-fn (::mdl/input task)
                                                  #(client/send-task-heartbeat (::mdl/task-token task)))
                                      (catch Throwable e e))]
                      (handle-result activity-arn task result)))

                  ;; thread is interrupted
                  (async/>!! chan :done)
                  (async/close! chan))]))

(defn poll-with-backoff [poll activity-arn consec-poll-fail]
  (async/go (async/<! (async/timeout (min (* consec-poll-fail 200)
                                          4000)))
            (async/<! (poll activity-arn))))

(defn boot-worker
  ([terminate-mult activity-arn handler-fn]
   (boot-worker terminate-mult activity-arn handler-fn poll handle))
  ([terminate-mult activity-arn handler-fn poll handle]
   (let [terminate-chan (async/chan)]
     (async/tap terminate-mult terminate-chan)
     (async/go-loop [[message channel] (async/alts! [terminate-chan (poll activity-arn)])
                     handler-future nil
                     handler-chan nil
                     consec-poll-fail 0
                     state ::polling]
       (condp = state
         ::polling
         (cond
           (= channel terminate-chan)
           :done

           (and (instance? AmazonServiceException message)
                (= (.getErrorCode ^AmazonServiceException message)
                   "ActivityDoesNotExist"))
           (do (log/error "Polling terminated for non-existent activity"
                          {:arn activity-arn})
               message)

           (instance? Throwable message)
           (recur (async/alts! [terminate-chan (poll-with-backoff poll
                                                                  activity-arn
                                                                  consec-poll-fail)])
                  nil
                  nil
                  (+ consec-poll-fail 1)
                  ::polling)

           (= message {})
           (recur (async/alts! [terminate-chan (poll activity-arn)])
                  nil
                  nil
                  0
                  ::polling)

           :else
           (let [[handler-chan handler-future] (handle activity-arn
                                                       message
                                                       handler-fn)]
             (recur (async/alts! [terminate-chan handler-chan])
                    handler-future
                    handler-chan
                    0
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
                    consec-poll-fail
                    ::shutting-down))

           :else
           (recur (async/alts! [terminate-chan (poll activity-arn)])
                  nil
                  nil
                  consec-poll-fail
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

(defn warn-if-concurrency-larger-than-connection-pool [activity-arn->concurrency]
  (let [max-connections   (client/get-client-max-connections)
        total-concurrency (apply + (vals activity-arn->concurrency))]
    (when (< max-connections total-concurrency)
      (log/warnf (str "Total number of workers concurrency [%d] is larger than aws client's max "
                      "connections [%d]. There is a chance that connections might run out."
                      total-concurrency max-connections)))))

(defn boot [activity-arn->handler-fn activity-arn->concurrency]

  (warn-if-concurrency-larger-than-connection-pool activity-arn->concurrency)

  (let [terminate-chan (async/chan)
        terminate-mult (async/mult terminate-chan)
        exit-chans     (into #{}
                             (mapcat (fn [[activity-arn handler-fn]]
                                       (boot-workers terminate-mult
                                                     activity-arn
                                                     handler-fn
                                                     (get activity-arn->concurrency
                                                          activity-arn
                                                          default-activity-concurrency))))
                             activity-arn->handler-fn)]
    (maps/syms->map terminate-chan
                    exit-chans)))

