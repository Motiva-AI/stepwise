(ns stepwise.serialization
  (:require [pipeline-transit.core :as transit]
            [clojure.string :as strs]
            [clojure.pprint :as pprint]))

(defn deser-io-doc [transit-json]
  (transit/read-transit-json transit-json))

(defn ser-io-doc [data]
  (transit/write-transit-json data))

(defn ser-keyword-name [kw-name]
  (-> kw-name
      str
      (strs/replace #"^:" "")
      (strs/replace #"/" "__")))

(defn deser-keyword-name [kw-name]
  (-> kw-name
      (strs/replace #"__" "/")
      keyword))

(defn ser-exception [e]
  (pprint/write (Throwable->map e) :stream nil))

(defn ser-error-val [error]
  (if (keyword? error)
    (strs/replace (str error) #"^:" "")
    (str error)))

