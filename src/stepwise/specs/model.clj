(ns stepwise.specs.model
  (:require [stepwise.model :as mdl]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]))

(defn type= [type-name]
  (s/with-gen #(-> % :type (= type-name))
              (constantly (gen/return {:type type-name}))))

(s/def ::mdl/fail
  (s/merge (type= ::mdl/fail)
           (s/keys :req-un [::mdl/error ::mdl/cause]
                   :opt-un [::mdl/comment])))

(s/def ::mdl/succeed
  (s/merge (type= ::mdl/succeed)
           (s/keys :opt-un [::mdl/comment ::mdl/input-path ::mdl/output-path])))

(s/def ::json-path string?)
(s/def ::mdl/reference-path ::json-path)
(s/def ::mdl/input-path ::json-path)
(s/def ::mdl/output-path ::json-path)
(s/def ::mdl/result-path ::json-path)
(s/def ::mdl/seconds-path ::json-path)
(s/def ::mdl/timestamp-path ::json-path)

