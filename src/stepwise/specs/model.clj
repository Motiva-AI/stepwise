(ns stepwise.specs.model
  (:require [stepwise.model :as mdl]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]))

(s/def ::json-path (s/and string? not-empty))
(s/def ::mdl/reference-path ::json-path)
(s/def ::mdl/input-path ::json-path)
(s/def ::mdl/output-path ::json-path)
(s/def ::mdl/result-path ::json-path)
(s/def ::mdl/seconds-path ::json-path)
(s/def ::mdl/timestamp-path ::json-path)

(s/def ::mdl/heartbeat-seconds int?)
(s/def ::mdl/timeout-seconds int?)
(s/def ::mdl/interval-seconds int?)
(s/def ::mdl/max-attempts int?)
(s/def ::mdl/backoff-rate int?)
(s/def ::mdl/comment string?)
(s/def ::mdl/cause string?)

(defn activity-arn? [arn]
  (re-find #"^arn:[a-z0-9-]+:states:[a-z0-9-]+:[0-9]+:activity:[^:]{1,80}$"
           arn))

(defn function-arn? [arn]
  (re-find #"^arn:[a-z0-9-]+:lambda:[a-z0-9-]+:[0-9]+:function:([a-zA-Z0-9-_]+)(:(\$LATEST|[a-zA-Z0-9-_]+))?$"
           arn))

(s/def ::mdl/resource (s/or :activity-arn activity-arn?
                            :function-arn function-arn?))

