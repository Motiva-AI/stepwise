(ns stepwise.reloaded
  (:require [stepwise.client :as client]
            [stepwise.core :as core]
            [stepwise.activities :as activities]
            [clojure.set :as sets]
            [stepwise.arns :as arns])
  (:import (java.util UUID)))

(defn machine-name->snapshot [machine-name]
  (str (when-let [mns (namespace machine-name)]
         (str mns "-"))
       (name machine-name)
       "-"
       (str (System/currentTimeMillis))))

(defn activity-name->snapshot [snapshot-name activity-names]
  (into {}
        (map (fn [activity-name]
               [activity-name (keyword (str snapshot-name
                                            (when-let [ans (namespace activity-name)]
                                              (str "-" ans)))
                                       (name activity-name))]))
        activity-names))

(defn start-execution!! [machine-name definition task-handlers input]
  (let [snapshot-name      (machine-name->snapshot machine-name)
        activity-names     (activities/get-names definition)
        activity->snapshot (activity-name->snapshot snapshot-name activity-names)
        definition         (activities/resolve-kw-resources activity->snapshot
                                                            definition)
        snapshot-arn       (core/ensure-state-machine snapshot-name definition)
        workers            (core/start-workers! (sets/rename-keys task-handlers
                                                                  activity->snapshot))
        result             (core/start-execution!! snapshot-name
                                                   {:input          input
                                                    :execution-name (str (UUID/randomUUID))})]
    (core/kill-workers workers)
    (doseq [[_ snapshot-name] activity->snapshot]
      (client/delete-activity (arns/get-activity-arn snapshot-name)))
    (client/delete-state-machine snapshot-arn)
    result))

