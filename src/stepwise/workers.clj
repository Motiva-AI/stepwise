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
    (future (async/>!! chan (try (not-empty (client/get-activity-task activity-arn))
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

(defn handle [activity-arn task handler-fn]
  (let [chan (async/chan)]
    [chan (future (let [result (try (if (-> handler-fn meta :heartbeat?)
                                      (handler-fn (::mdl/input task)
                                                  #(client/send-task-heartbeat
                                                     (::mdl/task-token task)))
                                      (handler-fn (::mdl/input task)))
                                    (catch Throwable e e))]
                    (when-not (.isInterrupted (Thread/currentThread))
                      (try
                        (if (instance? Throwable result)
                          (do
                            (log/warn result
                                      "Activity task failed"
                                      (prn-str (maps/syms->map activity-arn task)))
                            (client/send-task-failure (::mdl/task-token task)
                                                      (exception->failure-map result)))
                          (do
                            (log/debug "Activity task completed"
                                       (prn-str (maps/syms->map activity-arn task result)))
                            (client/send-task-success (::mdl/task-token task)
                                                      result)))
                        (catch Throwable e
                          (log/error e
                                     "Failed to send activity task result"
                                     (prn-str (maps/syms->map activity-arn task result)))))))
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
           (do (log/error message
                          "Polling terminated for non-existent activity"
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

           (nil? message)
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

(defn boot [activity-arn->handler-fn & [activity-arn->concurrency]]
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

