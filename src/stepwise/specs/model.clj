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

; TODO needs to be a valid arn
(s/def ::mdl/resource string?)

