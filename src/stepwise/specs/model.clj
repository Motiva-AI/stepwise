(ns stepwise.specs.model
  (:require [stepwise.model :as mdl]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def ::json-path (s/and string? not-empty))
(s/def ::mdl/reference-path ::json-path)

;; https://docs.aws.amazon.com/step-functions/latest/dg/concepts-input-output-filtering.html
(s/def ::mdl/input-path ::json-path)
(s/def ::mdl/parameters ::json-path)
(s/def ::mdl/result-selector ::json-path)
(s/def ::mdl/result-path ::json-path)
(s/def ::mdl/output-path ::json-path)

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

(def dummy-activity-arn
  "arn:aws:states:us-west-2:000000000000:activity:gogogo")

(defn function-arn? [arn]
  (re-find #"^arn:[a-z0-9-]+:lambda:[a-z0-9-]+:[0-9]+:function:([a-zA-Z0-9-_]+)(:(\$LATEST|[a-zA-Z0-9-_]+))?$"
           arn))

(def dummy-function-arn
  "arn:aws:lambda:us-west-2:000000000000:function:gogogo")

(s/def ::mdl/resource (s/or :activity-arn (s/with-gen activity-arn?
                                                      (constantly (gen/return dummy-activity-arn)))
                            :function-arn (s/with-gen function-arn?
                                                      (constantly (gen/return dummy-function-arn)))))

